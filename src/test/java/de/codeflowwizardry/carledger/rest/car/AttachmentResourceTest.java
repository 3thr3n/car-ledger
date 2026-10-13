package de.codeflowwizardry.carledger.rest.car;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.attachment.AttachmentService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.MiscellaneousBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.miscellaneous.MiscellaneousBillInput;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@QuarkusTest
class AttachmentResourceTest
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
	AttachmentService attachmentService;

	long carId;
	long billId;

	@BeforeEach
	@Transactional
	void setup()
	{
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
				carId = car.getId();
				billId = miscellaneousBillFactory.create(
						new MiscellaneousBillInput(BigDecimal.TEN, 19, LocalDate.of(2025, 1, 1), "Parking"),
						carId, user).getBill().getId();
			}
		}
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldAttachListAndServeAFile()
	{
		int id = upload("receipt.jpg", JPEG, "image/jpeg")
				.statusCode(201)
				.body("fileName", is("receipt.jpg"))
				.body("contentType", is("image/jpeg"))
				.body("size", is(JPEG.length))
				.extract().path("id");

		given()
				.get("/api/attachment/{carId}/bill/{billId}", carId, billId)
				.then()
				.statusCode(200)
				.body("size()", is(1))
				.body("[0].id", is(id));

		given()
				.get("/api/bill/{carId}/miscellaneous/all", carId)
				.then()
				.statusCode(200)
				.body("data[0].attachments", is(1));

		byte[] content = given()
				.get("/api/attachment/{carId}/{id}/content", carId, id)
				.then()
				.statusCode(200)
				.contentType("image/jpeg")
				.header("Content-Disposition", is("inline; filename*=UTF-8''receipt.jpg"))
				.header("X-Content-Type-Options", is("nosniff"))
				.header("Content-Security-Policy", containsString("sandbox"))
				.extract().asByteArray();
		assertArrayEquals(JPEG, content);
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldUseTheTypeOfTheContent()
	{
		// a PDF named like an image and sent as text
		upload("scan.png", PDF, "text/plain")
				.statusCode(201)
				.body("contentType", is("application/pdf"));
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldRejectOtherTypesEmptyAndLargeFiles()
	{
		upload("page.html", "<html><script>alert(1)</script>".getBytes(), "text/html")
				.statusCode(400)
				.body("[0]", containsString("JPEG, PNG, WebP and PDF"));

		upload("empty.jpg", new byte[0], "image/jpeg")
				.statusCode(400)
				.body("[0]", containsString("empty"));

		// the tests allow 64 KB
		byte[] large = Arrays.copyOf(JPEG, 65 * 1024);
		upload("large.jpg", large, "image/jpeg")
				.statusCode(400)
				.body("[0]", containsString("larger"));

		assertEquals(0, attachmentRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldLimitTheAttachmentsPerBill()
	{
		// the tests allow 3
		for (int i = 0; i < 3; i++)
		{
			upload("receipt" + i + ".jpg", JPEG, "image/jpeg").statusCode(201);
		}
		upload("receipt3.jpg", JPEG, "image/jpeg")
				.statusCode(400)
				.body("[0]", containsString("at most 3"));
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldDeleteTheAttachmentWithItsContent()
	{
		int id = upload("receipt.jpg", JPEG, "image/jpeg").statusCode(201).extract().path("id");

		given()
				.delete("/api/attachment/{carId}/{id}", carId, id)
				.then()
				.statusCode(204);

		assertEquals(0, attachmentRepository.count());
		assertEquals(0L, countContents());
		given()
				.get("/api/attachment/{carId}/{id}/content", carId, id)
				.then()
				.statusCode(404);
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldDeleteTheAttachmentsWithTheBillAndTheCar()
	{
		upload("receipt.jpg", JPEG, "image/jpeg").statusCode(201);

		given()
				.delete("/api/bill/{carId}/{billId}", carId, billId)
				.then()
				.statusCode(204);
		assertEquals(0, attachmentRepository.count());
		assertEquals(0L, countContents());
	}

	@Test
	@TestSecurity(user = "paul", roles = "user")
	void shouldNotShowTheAttachmentsToOtherUsers()
	{
		long id = attachAsPeter();

		given()
				.get("/api/attachment/{carId}/bill/{billId}", carId, billId)
				.then()
				.statusCode(404);
		given()
				.get("/api/attachment/{carId}/{id}/content", carId, id)
				.then()
				.statusCode(404);
		given()
				.delete("/api/attachment/{carId}/{id}", carId, id)
				.then()
				.statusCode(404);
		upload("receipt.jpg", JPEG, "image/jpeg").statusCode(404);

		assertEquals(1, attachmentRepository.count());
	}

	long attachAsPeter()
	{
		return attachmentService.addToBill(carId, billId, "peter", "receipt.jpg", JPEG).getId();
	}

	@Transactional
	long countContents()
	{
		return entityManager.createQuery("select count(c) from AttachmentContent c", Long.class).getSingleResult();
	}

	private ValidatableResponse upload(String name, byte[] content, String type)
	{
		return given()
				.multiPart("file", name, content, type)
				.post("/api/attachment/{carId}/bill/{billId}", carId, billId)
				.then();
	}
}
