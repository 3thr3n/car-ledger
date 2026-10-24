package de.codeflowwizardry.carledger.rest.car.inspection;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.TechnicalInspectionRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Peter's Golf passes its HUs, once only after a Nachprüfung
 */
@QuarkusTest
@TestSecurity(user = "peter", roles = {
		"user"
})
class TechnicalInspectionResourceTest
{
	private static final String HUS = "/api/technical-inspection/{carId}";
	private static final String HU = HUS + "/{id}";

	private static final byte[] PDF = "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII);

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	TechnicalInspectionRepository inspectionRepository;

	@Inject
	AttachmentRepository attachmentRepository;

	long carId;
	long otherCarId;

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();
		carId = car(account("peter"), "Peters car");
		otherCarId = car(account("paul"), "Pauls car");
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	private AccountEntity account(String name)
	{
		AccountEntity account = new AccountEntity();
		account.setUserId(name);
		account.setMaxCars(2);
		accountRepository.persist(account);
		return account;
	}

	private long car(AccountEntity account, String name)
	{
		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName(name);
		carRepository.persist(car);
		return car.getId();
	}

	private static Map<String, Object> hu(String date, String result, String nextDue)
	{
		Map<String, Object> input = new HashMap<>();
		input.put("date", date);
		input.put("odometer", 80_000);
		input.put("testingBody", " TÜV Süd ");
		input.put("result", result);
		input.put("nextDue", nextDue);
		return input;
	}

	private int add(Map<String, Object> input)
	{
		return given()
				.contentType(ContentType.JSON)
				.body(input)
				.when()
				.put(HUS, carId)
				.then()
				.statusCode(202)
				.extract()
				.path("id");
	}

	private io.restassured.response.ValidatableResponse reminders()
	{
		return given().when().get("/api/reminder/{carId}", carId).then().statusCode(200);
	}

	@Test
	void shouldAddAndMoveTheReminderOfTheHu()
	{
		given()
				.contentType(ContentType.JSON)
				.body(hu("2024-05-10", "NO_DEFECTS", "2026-05-17"))
				.when()
				.put(HUS, carId)
				.then()
				.statusCode(202)
				.body("testingBody", is("TÜV Süd"))
				.body("passed", is(true))
				// the first of the month
				.body("nextDue", is("2026-05-01"));
		// due at the end of the month
		reminders().body("title", contains("HU")).body("[0].dueDate", is("2026-05-31"));

		// after major defects, the Nachprüfung a few weeks later
		add(hu("2026-05-04", "MAJOR_DEFECTS", null));
		Map<String, Object> retest = hu("2026-05-20", "NO_DEFECTS", "2028-05-01");
		retest.put("defects", "Brake disc front left");
		add(retest);
		reminders().body("size()", is(1)).body("[0].dueDate", is("2028-05-31"));

		// an older one entered later doesn't move it back
		add(hu("2022-05-02", "MINOR_DEFECTS", "2024-05-01"));
		reminders().body("[0].dueDate", is("2028-05-31"));

		given().when().get(HUS, carId).then()
				.statusCode(200)
				.body("date", contains("2026-05-20", "2026-05-04", "2024-05-10", "2022-05-02"))
				.body("passed", contains(true, false, true, true))
				.body("[0].defects", is("Brake disc front left"));
	}

	@Test
	void shouldBringBackADeletedReminderWithTheNextHu()
	{
		add(hu("2024-05-10", "NO_DEFECTS", "2026-05-01"));
		int reminder = reminders().extract().path("[0].id");
		given().when().delete("/api/reminder/{carId}/{id}", carId, reminder).then().statusCode(204);

		add(hu("2026-05-11", "NO_DEFECTS", "2028-05-01"));

		reminders().body("title", contains("HU")).body("[0].dueDate", is("2028-05-31"));
	}

	@Test
	void shouldKeepTheReminderWhenDeleted()
	{
		int id = add(hu("2024-05-10", "NO_DEFECTS", "2026-05-01"));

		given().when().delete(HU, carId, id).then().statusCode(204);
		given().when().delete(HU, carId, id).then().statusCode(404);

		given().when().get(HUS, carId).then().body("size()", is(0));
		reminders().body("[0].dueDate", is("2026-05-31"));
	}

	@Test
	void shouldUpdateAndMoveTheReminder()
	{
		int id = add(hu("2024-05-10", "NO_DEFECTS", "2026-05-01"));

		given()
				.contentType(ContentType.JSON)
				.body(hu("2024-05-10", "MINOR_DEFECTS", "2026-06-01"))
				.when()
				.post(HU, carId, id)
				.then()
				.statusCode(200)
				.body("result", is("MINOR_DEFECTS"))
				.body("passed", is(true));
		reminders().body("[0].dueDate", is("2026-06-30"));
	}

	@Test
	void shouldRejectInvalidInput()
	{
		Map<String, Object> input = hu(LocalDate.now().plusDays(1).toString(), null, null);
		input.put("odometer", -1);
		input.put("defects", "x".repeat(2001));
		given()
				.contentType(ContentType.JSON)
				.body(input)
				.when()
				.put(HUS, carId)
				.then()
				.statusCode(400)
				.body("", containsInAnyOrder("Date must not be in the future!", "Result must be set!",
						"Odometer must be between 0 and 10000000!",
						"Defects must not be longer than 2000 characters!"));

		given()
				.contentType(ContentType.JSON)
				.body(hu("2024-05-10", "NO_DEFECTS", "2024-05-01"))
				.when()
				.put(HUS, carId)
				.then()
				.statusCode(400)
				.body("", contains("Next due month must be after the month of the inspection!"));
	}

	@Test
	void shouldKeepTheReportAndDeleteItWithTheHu()
	{
		int id = add(hu("2024-05-10", "NO_DEFECTS", "2026-05-01"));

		given()
				.multiPart("file", "report.pdf", PDF, "application/pdf")
				.when()
				.post("/api/attachment/{carId}/technical-inspection/{id}", carId, id)
				.then()
				.statusCode(201)
				.body("fileName", is("report.pdf"));
		given().when().get("/api/attachment/{carId}/technical-inspection/{id}", carId, id).then()
				.statusCode(200)
				.body("fileName", contains("report.pdf"));
		given().when().get(HUS, carId).then().body("[0].documents", is(1));

		given().when().delete(HU, carId, id).then().statusCode(204);
		long left = QuarkusTransaction.requiringNew().call(attachmentRepository::count);
		assertEquals(0, left);
	}

	@Test
	void shouldNotTouchTheHusOfOtherUsers()
	{
		given()
				.contentType(ContentType.JSON)
				.body(hu("2024-05-10", "NO_DEFECTS", null))
				.when()
				.put(HUS, otherCarId)
				.then()
				.statusCode(404);

		int id = add(hu("2024-05-10", "NO_DEFECTS", null));

		// the id of peters HU below the car of paul
		given()
				.contentType(ContentType.JSON)
				.body(hu("2024-05-10", "NO_DEFECTS", null))
				.when()
				.post(HU, otherCarId, id)
				.then()
				.statusCode(404);
		given().when().delete(HU, otherCarId, id).then().statusCode(404);
		given().when().get(HUS, otherCarId).then().statusCode(200).body("size()", is(0));
		given().when().get("/api/attachment/{carId}/technical-inspection/{id}", otherCarId, id).then()
				.statusCode(404);
	}

	@Test
	void shouldBeDeletedWithTheCar()
	{
		add(hu("2024-05-10", "NO_DEFECTS", "2026-05-01"));

		QuarkusTransaction.requiringNew().run(() -> carRepository.deleteForUser(carId, "peter"));

		long left = QuarkusTransaction.requiringNew().call(inspectionRepository::count);
		assertEquals(0, left);
	}
}
