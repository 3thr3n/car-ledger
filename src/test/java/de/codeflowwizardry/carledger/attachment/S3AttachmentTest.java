package de.codeflowwizardry.carledger.attachment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.AttachmentContentEntity;
import de.codeflowwizardry.carledger.data.AttachmentEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.Preview;
import de.codeflowwizardry.carledger.data.factory.MiscellaneousBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.miscellaneous.MiscellaneousBillInput;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Attachments in S3: stored, served, deleted with whatever deletes them, moved
 * from the database and part of the backup
 */
@QuarkusTest
@TestProfile(S3Profile.class)
@TestSecurity(user = "peter", roles = "user")
class S3AttachmentTest
{
	/** start of a JPEG, enough for the type check */
	private static final byte[] JPEG = {
			(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'
	};
	private static final byte[] PDF = "%PDF-1.7\n%%EOF".getBytes();

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	AttachmentRepository attachmentRepository;

	@Inject
	MiscellaneousBillFactory miscellaneousBillFactory;

	@Inject
	EntityManager entityManager;

	@Inject
	AttachmentCleanup cleanup;

	@Inject
	AttachmentMover mover;

	@Inject
	S3Client s3;

	long carId;
	long billId;

	@BeforeEach
	@Transactional
	void setup()
	{
		AccountEntity account = new AccountEntity();
		account.setUserId("peter");
		account.setMaxCars(1);
		accountRepository.persist(account);

		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName("peters car");
		carRepository.persist(car);
		carId = car.getId();
		billId = miscellaneousBillFactory.create(
				new MiscellaneousBillInput(BigDecimal.TEN, 19, LocalDate.of(2025, 1, 1), "Parking"),
				carId, "peter").getBill().getId();
	}

	@AfterEach
	void cleanup()
	{
		QuarkusTransaction.requiringNew().run(() -> accountRepository.deleteAll());
		cleanup.removeQueued(100);
		for (String key : objects())
		{
			s3.deleteObject(request -> request.bucket(S3Profile.BUCKET).key(key));
		}
	}

	@Test
	void shouldStoreAndServeTheFileInTheBucket()
	{
		long id = upload(JPEG);

		AttachmentEntity attachment = QuarkusTransaction.requiringNew()
				.call(() -> attachmentRepository.findById(id));
		assertEquals(S3AttachmentStorage.NAME, attachment.getStorage());
		assertTrue(attachment.getStorageKey().startsWith("attachments/"));
		assertEquals(List.of(attachment.getStorageKey()), objects());

		assertArrayEquals(JPEG, content(id));
	}

	@Test
	void shouldDeleteTheObjectWithTheAttachment()
	{
		long id = upload(JPEG);

		given().delete("/api/attachment/{carId}/{id}", carId, id).then().statusCode(204);
		assertEquals(1, objects().size(), "deleted after the commit by the job");
		assertEquals(1, removals());

		assertEquals(1, cleanup.removeQueued(100));
		assertEquals(List.of(), objects());
		assertEquals(0, removals());
	}

	@Test
	void shouldDeleteTheObjectsWithTheCar()
	{
		upload(JPEG);
		upload(PDF);

		given().delete("/api/car/my/{id}", carId).then().statusCode(204);
		assertEquals(2, removals(), "queued by the database for the foreign keys");

		// in batches
		assertEquals(2, cleanup.removeQueued(1));
		assertEquals(List.of(), objects());
	}

	@Test
	void shouldKeepThePreviewNextToTheFile()
	{
		long id = upload(TestImages.png(800, 600));
		String key = QuarkusTransaction.requiringNew().call(() -> attachmentRepository.findById(id).getStorageKey());
		assertEquals(List.of(key, key + ".preview"), objects());

		byte[] preview = given().get("/api/attachment/{carId}/{id}/preview", carId, id).then().statusCode(200)
				.contentType("image/jpeg").extract().asByteArray();
		assertEquals(256, TestImages.read(preview).getWidth());

		s3.putObject(request -> request.bucket(S3Profile.BUCKET).key("attachments/orphan.preview"),
				RequestBody.fromBytes(PDF));
		assertEquals(1, cleanup.removeOrphans(Instant.now().plus(AttachmentCleanup.ORPHAN_AGE).plusSeconds(60)),
				"only the preview without a file");
		assertEquals(List.of(key, key + ".preview"), objects());

		given().delete("/api/attachment/{carId}/{id}", carId, id).then().statusCode(204);
		assertEquals(1, cleanup.removeQueued(100));
		assertEquals(List.of(), objects());
	}

	@Test
	void shouldGenerateThePreviewAgainAfterTheMove()
	{
		long id = QuarkusTransaction.requiringNew().call(() -> {
			AttachmentEntity attachment = new AttachmentEntity();
			attachment.setCar(entityManager.find(CarEntity.class, carId));
			attachment.setFileName("old.png");
			attachment.setContentType("image/png");
			attachment.setSize(1);
			attachment.setStorage(DatabaseAttachmentStorage.NAME);
			attachment.setPreview(Preview.JPEG);
			attachmentRepository.persistAndFlush(attachment);
			AttachmentContentEntity content = new AttachmentContentEntity(attachment.getId(),
					TestImages.png(600, 600));
			content.setPreview(PDF);
			entityManager.persist(content);
			return attachment.getId();
		});

		assertEquals(1, mover.move().moved());
		assertEquals(1, objects().size(), "no preview yet");

		byte[] preview = given().get("/api/attachment/{carId}/{id}/preview", carId, id).then().statusCode(200)
				.contentType("image/jpeg").extract().asByteArray();
		assertEquals(256, TestImages.read(preview).getWidth());
		assertEquals(2, objects().size());
	}

	@Test
	void shouldDeleteOnlyOldObjectsWithoutAnAttachment()
	{
		upload(JPEG);
		s3.putObject(request -> request.bucket(S3Profile.BUCKET).key("attachments/orphan"),
				RequestBody.fromBytes(PDF));
		s3.putObject(request -> request.bucket(S3Profile.BUCKET).key("other/file"), RequestBody.fromBytes(PDF));

		assertEquals(0, cleanup.removeOrphans(Instant.now()), "may belong to a running transaction");
		assertEquals(1, cleanup.removeOrphans(Instant.now().plus(AttachmentCleanup.ORPHAN_AGE).plusSeconds(60)));

		List<String> left = objects();
		assertEquals(2, left.size(), "the file of the attachment and the object outside the prefix");
		assertFalse(left.contains("attachments/orphan"));
		s3.deleteObject(request -> request.bucket(S3Profile.BUCKET).key("other/file"));
	}

	@Test
	void shouldMoveTheFilesOfTheDatabase()
	{
		long id = QuarkusTransaction.requiringNew().call(() -> {
			AttachmentEntity attachment = new AttachmentEntity();
			attachment.setCar(entityManager.find(CarEntity.class, carId));
			attachment.setFileName("old.pdf");
			attachment.setContentType("application/pdf");
			attachment.setSize(PDF.length);
			attachment.setStorage(DatabaseAttachmentStorage.NAME);
			attachmentRepository.persistAndFlush(attachment);
			entityManager.persist(new AttachmentContentEntity(attachment.getId(), PDF));
			return attachment.getId();
		});

		AttachmentMover.Summary summary = mover.move();
		assertEquals(new AttachmentMover.Summary(1, List.of(), 0), summary);

		QuarkusTransaction.requiringNew().run(() -> {
			AttachmentEntity attachment = attachmentRepository.findById(id);
			assertEquals(S3AttachmentStorage.NAME, attachment.getStorage());
			assertNotNull(attachment.getStorageKey());
			assertNull(entityManager.find(AttachmentContentEntity.class, id));
		});
		assertEquals(0, removals(), "nothing outside the database to delete");
		assertArrayEquals(PDF, content(id));

		assertEquals(new AttachmentMover.Summary(0, List.of(), 0), mover.move());
	}

	@Test
	void shouldQueueTheObjectOfAFileMovedToTheDatabase()
	{
		long id = upload(JPEG);

		QuarkusTransaction.requiringNew().run(() -> {
			AttachmentEntity attachment = attachmentRepository.findById(id);
			attachment.setStorage(DatabaseAttachmentStorage.NAME);
			attachment.setStorageKey(null);
			entityManager.persist(new AttachmentContentEntity(id, JPEG));
		});
		assertEquals(1, removals());

		assertEquals(1, cleanup.removeQueued(100));
		assertEquals(List.of(), objects());
		assertArrayEquals(JPEG, content(id));
	}

	@Test
	void shouldKeepAFileThatCannotBeMoved()
	{
		long id = QuarkusTransaction.requiringNew().call(() -> {
			AttachmentEntity attachment = new AttachmentEntity();
			attachment.setCar(entityManager.find(CarEntity.class, carId));
			attachment.setFileName("lost.pdf");
			attachment.setContentType("application/pdf");
			attachment.setSize(PDF.length);
			attachment.setStorage(DatabaseAttachmentStorage.NAME);
			attachmentRepository.persistAndFlush(attachment);
			return attachment.getId();
		});

		assertEquals(new AttachmentMover.Summary(0, List.of(id), 1), mover.move());
		assertEquals(DatabaseAttachmentStorage.NAME, QuarkusTransaction.requiringNew()
				.call(() -> attachmentRepository.findById(id).getStorage()));
		assertEquals(List.of(), objects());
	}

	@Test
	void shouldWriteTheFilesIntoTheBackup() throws IOException
	{
		upload(JPEG);

		byte[] zip = given().get("/api/user/me/backup").then().statusCode(200).extract().asByteArray();
		boolean found = false;
		try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip)))
		{
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null)
			{
				if (!entry.getName().equals("backup.json"))
				{
					assertArrayEquals(JPEG, in.readAllBytes());
					found = true;
				}
			}
		}
		assertTrue(found);
	}

	@Test
	@TestSecurity(user = "alice", roles = {
			"user", "admin"
	})
	void shouldShowTheFilesPerStorage()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			AccountEntity admin = new AccountEntity();
			admin.setUserId("alice");
			accountRepository.persist(admin);
		});
		QuarkusTransaction.requiringNew().run(() -> {
			AttachmentEntity attachment = new AttachmentEntity();
			attachment.setCar(entityManager.find(CarEntity.class, carId));
			attachment.setFileName("old.pdf");
			attachment.setContentType("application/pdf");
			attachment.setSize(PDF.length);
			attachment.setStorage(DatabaseAttachmentStorage.NAME);
			attachmentRepository.persistAndFlush(attachment);
			entityManager.persist(new AttachmentContentEntity(attachment.getId(), PDF));
		});

		given().get("/api/admin/stats").then().statusCode(200)
				.body("fileStorage", is("S3"))
				.body("files.size()", is(1))
				.body("files[0].storage", is("DB"))
				.body("files[0].files", is(1))
				.body("files[0].bytes", is(PDF.length));
	}

	private long upload(byte[] content)
	{
		return given()
				.multiPart("file", "receipt", content, "application/octet-stream")
				.post("/api/attachment/{carId}/bill/{billId}", carId, billId)
				.then()
				.statusCode(201)
				.extract().<Integer>path("id")
				.longValue();
	}

	private byte[] content(long id)
	{
		return given().get("/api/attachment/{carId}/{id}/content", carId, id).then().statusCode(200)
				.extract().asByteArray();
	}

	private List<String> objects()
	{
		return s3.listObjectsV2(request -> request.bucket(S3Profile.BUCKET)).contents().stream()
				.map(S3Object::key)
				.toList();
	}

	private long removals()
	{
		return QuarkusTransaction.requiringNew().call(() -> ((Number) entityManager
				.createNativeQuery("SELECT count(*) FROM attachment_removal").getSingleResult()).longValue());
	}
}
