package de.codeflowwizardry.carledger.rest.car.recurring;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.attachment.TestImages;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.BillCategory;
import de.codeflowwizardry.carledger.data.BillInterval;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.RecurringBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Files of the recurring costs, e.g. the contract of a leasing
 */
@QuarkusTest
class RecurringBillFilesTest
{
	private static final byte[] PDF = "%PDF-1.7\n%%EOF".getBytes();

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	AttachmentRepository attachmentRepository;

	@Inject
	RecurringBillFactory recurringBillFactory;

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
				billId = recurringBillFactory.create(carId, new RecurringBillInput("Leasing", null,
						BillInterval.MONTHLY, BillCategory.FINANCE, LocalDate.of(2025, 1, 1), null,
						BigDecimal.valueOf(299)), user).getId();
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
	void shouldAttachListAndServeTheContract()
	{
		int id = upload("contract.pdf", PDF)
				.statusCode(201)
				.body("fileName", is("contract.pdf"))
				.body("contentType", is("application/pdf"))
				.extract().path("id");

		given().get("/api/attachment/{carId}/recurring/{billId}", carId, billId).then()
				.statusCode(200)
				.body("size()", is(1))
				.body("[0].id", is(id));

		byte[] content = given().get("/api/attachment/{carId}/{id}/content", carId, id).then()
				.statusCode(200)
				.contentType("application/pdf")
				.extract().asByteArray();
		assertArrayEquals(PDF, content);
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldShowTheFilesInTheList()
	{
		given().get("/api/bill/{carId}/recurring", carId).then().statusCode(200)
				.body("data[0].attachments", is(0))
				.body("data[0].photo", nullValue());

		upload("contract.pdf", PDF).statusCode(201);
		int photo = upload("card.png", TestImages.png(300, 200)).statusCode(201).extract().path("id");

		given().get("/api/bill/{carId}/recurring", carId).then().statusCode(200)
				.body("data[0].attachments", is(2))
				.body("data[0].photo", is(photo));
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldLimitTheFilesLikeAnEntry()
	{
		for (int i = 0; i < 3; i++)
		{
			upload("page" + i + ".pdf", PDF).statusCode(201);
		}

		upload("page3.pdf", PDF)
				.statusCode(400)
				.body("[0]", containsString("at most 3 files"));
		upload("notes.txt", "text".getBytes())
				.statusCode(400);
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldDeleteTheFilesWithTheRecurringCost()
	{
		upload("contract.pdf", PDF).statusCode(201);

		given().delete("/api/bill/{carId}/recurring/{billId}", carId, billId).then().statusCode(202);

		assertEquals(0, countFiles());
	}

	@Test
	@TestSecurity(user = "paul", roles = "user")
	void shouldNotShowTheFilesOfAnotherCar()
	{
		given().get("/api/attachment/{carId}/recurring/{billId}", carId, billId).then().statusCode(404);
		upload("contract.pdf", PDF).statusCode(404);
		assertEquals(0, countFiles());
	}

	@Transactional
	long countFiles()
	{
		return attachmentRepository.count();
	}

	private ValidatableResponse upload(String name, byte[] content)
	{
		return given()
				.multiPart("file", name, content, "application/octet-stream")
				.post("/api/attachment/{carId}/recurring/{billId}", carId, billId)
				.then();
	}
}
