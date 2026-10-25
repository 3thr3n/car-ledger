package de.codeflowwizardry.carledger.rest.car.modification;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.ModificationService;
import de.codeflowwizardry.carledger.attachment.AttachmentService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.ApprovalType;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.DocumentKind;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Peter keeps the Gutachten, the Abnahme and photos of his coilovers
 */
@QuarkusTest
class ModificationDocumentTest
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
	ModificationService modificationService;

	@Inject
	AttachmentService attachmentService;

	@Inject
	EntityManager entityManager;

	long carId;
	long modificationId;

	@BeforeEach
	void setup()
	{
		carId = createAccounts();
		modificationId = modificationService.create(carId, new ModificationInput("Coilovers", "KW",
				ApprovalType.PARTS_REPORT, null, null, null, null, false, null, null, null, null, null, null), "peter")
				.id();
	}

	@Transactional
	long createAccounts()
	{
		accountRepository.deleteAll();
		long peters = 0;
		for (String user : new String[] {
				"peter", "paul"
		})
		{
			AccountEntity account = new AccountEntity();
			account.setUserId(user);
			account.setMaxCars(1);
			accountRepository.persist(account);

			CarEntity car = new CarEntity();
			car.setUser(account);
			car.setName(user + "s car");
			carRepository.persist(car);
			if (user.equals("peter"))
			{
				peters = car.getId();
			}
		}
		return peters;
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	private ValidatableResponse upload(String name, byte[] content, String type, String kind)
	{
		RequestSpecification request = given().multiPart("file", name, content, type);
		if (kind != null)
		{
			request.multiPart("kind", kind);
		}
		return request.post("/api/attachment/{carId}/modification/{id}", carId, modificationId).then();
	}

	@Transactional
	long countContents()
	{
		return entityManager.createQuery("select count(c) from AttachmentContent c", Long.class).getSingleResult();
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldListTheDocumentsByTheirKind()
	{
		upload("front.jpg", JPEG, "image/jpeg", "PHOTO").statusCode(201).body("kind", is("PHOTO"));
		int gutachten = upload("gutachten.pdf", PDF, "application/pdf", "APPROVAL")
				.statusCode(201)
				.extract().path("id");
		upload("abnahme.pdf", PDF, "application/pdf", "INSPECTION").statusCode(201);

		given()
				.get("/api/attachment/{carId}/modification/{id}", carId, modificationId)
				.then()
				.statusCode(200)
				.body("kind", contains("APPROVAL", "INSPECTION", "PHOTO"))
				.body("fileName", contains("gutachten.pdf", "abnahme.pdf", "front.jpg"));
		given()
				.get("/api/modification/{carId}", carId)
				.then()
				.body("[0].documents", is(3));

		byte[] content = given()
				.get("/api/attachment/{carId}/{id}/content", carId, gutachten)
				.then()
				.statusCode(200)
				.contentType("application/pdf")
				.extract().asByteArray();
		assertArrayEquals(PDF, content);
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldRejectADocumentWithoutKindAndTooMany()
	{
		upload("gutachten.pdf", PDF, "application/pdf", null)
				.statusCode(400)
				.body("", contains("The kind of the document must be set!"));

		// the tests allow 3
		for (int i = 0; i < 3; i++)
		{
			upload("photo" + i + ".jpg", JPEG, "image/jpeg", "PHOTO").statusCode(201);
		}
		upload("photo3.jpg", JPEG, "image/jpeg", "PHOTO")
				.statusCode(400)
				.body("[0]", containsString("at most 3 documents"));
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldDeleteTheDocumentsWithTheModification()
	{
		upload("gutachten.pdf", PDF, "application/pdf", "APPROVAL").statusCode(201);

		given()
				.delete("/api/modification/{carId}/{id}", carId, modificationId)
				.then()
				.statusCode(204);
		assertEquals(0, attachmentRepository.count());
		assertEquals(0L, countContents());
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldDeleteADocument()
	{
		int id = upload("gutachten.pdf", PDF, "application/pdf", "APPROVAL").statusCode(201).extract().path("id");

		given()
				.delete("/api/attachment/{carId}/{id}", carId, id)
				.then()
				.statusCode(204);
		given()
				.get("/api/modification/{carId}", carId)
				.then()
				.body("[0].documents", is(0));
	}

	@Test
	@TestSecurity(user = "paul", roles = "user")
	void shouldNotShowTheDocumentsToOtherUsers()
	{
		long id = attachmentService.addToModification(carId, modificationId, "peter", "gutachten.pdf",
				DocumentKind.APPROVAL, PDF).getId();

		given()
				.get("/api/attachment/{carId}/modification/{id}", carId, modificationId)
				.then()
				.statusCode(404);
		given()
				.get("/api/attachment/{carId}/{id}/content", carId, id)
				.then()
				.statusCode(404);
		upload("photo.jpg", JPEG, "image/jpeg", "PHOTO").statusCode(404);

		assertEquals(1, attachmentRepository.count());
	}
}
