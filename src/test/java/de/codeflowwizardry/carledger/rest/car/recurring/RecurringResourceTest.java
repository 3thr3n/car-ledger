package de.codeflowwizardry.carledger.rest.car.recurring;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestHTTPEndpoint(RecurringResource.class)
class RecurringResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	Long carId;

	@BeforeEach
	@Transactional
	void setup()
	{
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setMaxCars(2);
		accountEntity.setUserId("peter");
		accountRepository.persist(accountEntity);

		CarEntity carEntity = new CarEntity();
		carEntity.setUser(accountEntity);
		carEntity.setName("Normal car");
		carRepository.persist(carEntity);

		carId = carEntity.getId();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllMyRecurringBills()
	{
		given()
				.pathParam("carId", carId)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("data.size()", is(0));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldAddNewMonthlyRecurringBill()
	{
		String body = """
				{
				     "name": "Finance payments",
					 "billInterval": "MONTHLY",
					 "category": "FINANCE",
					 "startDate": "2025-01-01",
					 "endDate": "2025-12-31",
					 "amount": 500.00
				 }
				""";

		given()
				.pathParam("carId", carId)
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(202)
				.body("total", is(6000.0F));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldAddNewRecurringBillOnce()
	{
		String body = """
				{
				     "name": "Finance payments",
					 "billInterval": "ONCE",
					 "category": "FINANCE",
					 "startDate": "2026-01-01",
					 "amount": 1000.00
				 }
				""";

		given()
				.pathParam("carId", carId)
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(202)
				.body("total", is(1000.0F))
				.body("finished", is(true));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldAddNewQuarterlyRecurringBill()
	{
		String body = """
				{
				     "name": "Finance payments",
					 "billInterval": "QUARTERLY",
					 "category": "FINANCE",
					 "startDate": "2025-01-01",
					 "endDate": "2025-12-31",
					 "amount": 500.00
				 }
				""";

		given()
				.pathParam("carId", carId)
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(202)
				.body("total", is(2000.0F));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldAddNewYearlyRecurringBill()
	{
		String body = """
				{
				     "name": "Finance payments",
					 "billInterval": "ANNUALLY",
					 "category": "FINANCE",
					 "startDate": "2025-01-01",
					 "endDate": "2025-12-31",
					 "amount": 500.00
				 }
				""";

		given()
				.pathParam("carId", carId)
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(202)
				.body("total", is(500.0F));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldNotCreateNewBill()
	{
		String body = """
				{
				     "name": "Finance payments",
					 "billInterval": "ANNUALLY",
					 "category": "FINANCE",
					 "startDate": "2025-01-01",
					 "endDate": "2025-12-31",
					 "amount": 500.00
				 }
				""";

		// not the car of peter, a fixed id would depend on the id sequence
		given()
				.pathParam("carId", carId + 1)
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(404);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldAddAndDeleteRecurringBill()
	{
		String body = """
				{
				     "name": "Finance payments",
					 "billInterval": "MONTHLY",
					 "category": "FINANCE",
					 "startDate": "2025-01-01",
					 "endDate": "2025-12-31",
					 "amount": 500.00
				 }
				""";

		Object id = given()
				.pathParam("carId", carId)
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(202)
				.extract()
				.jsonPath().get("id");

		given()
				.pathParam("carId", carId)
				.pathParam("billId", id)
				.contentType(ContentType.JSON)
				.when()
				.delete("{billId}")
				.then()
				.statusCode(202);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldNotDeleteRecurringBill()
	{
		given()
				.pathParam("carId", carId)
				.pathParam("billId", 99)
				.contentType(ContentType.JSON)
				.when()
				.delete("{billId}")
				.then()
				.statusCode(404);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldShowOnlyRunningRecurringBills()
	{
		String body = """
				{
				     "name": "Finance payments",
					 "billInterval": "MONTHLY",
					 "category": "FINANCE",
					 "startDate": "2025-01-01",
					 "endDate": "2025-12-31",
					 "amount": 500.00
				 }
				""";

		given()
				.pathParam("carId", carId)
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(202);

		given()
				.pathParam("carId", carId)
				.queryParam("onlyRunning", true)
				.accept(ContentType.JSON)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("data.size()", is(0))
				.body("total", is(0));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldShowOnlyRunningRecurringBills2()
	{
		String body = """
				{
				     "name": "Finance payments",
					 "billInterval": "MONTHLY",
					 "category": "FINANCE",
					 "startDate": "2025-01-01",
					 "endDate": "2026-12-31",
					 "amount": 500.00
				 }
				""";

		given()
				.pathParam("carId", carId)
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(202);

		given()
				.pathParam("carId", carId)
				.queryParam("onlyRunning", true)
				.accept(ContentType.JSON)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("data.size()", is(1))
				.body("total", is(1));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldCalculateNextDueDateCorrectly()
	{
		String body = """
				{
				     "name": "Finance payments",
					 "billInterval": "MONTHLY",
					 "category": "FINANCE",
					 "startDate": "2026-01-01",
					 "endDate": "2999-12-31",
					 "amount": 500.00
				 }
				""";

		LocalDate nextMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1);

		given()
				.pathParam("carId", carId)
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(202)
				.body("nextDueDate", is(nextMonth.format(DateTimeFormatter.ISO_DATE)));
	}

	private int createMonthlyBill()
	{
		return given()
				.pathParam("carId", carId)
				.contentType(ContentType.JSON)
				.body("""
						{
							"name": "Insurance",
							"billInterval": "MONTHLY",
							"category": "INSURANCE",
							"startDate": "2025-01-01",
							"amount": 100.00
						}
						""")
				.when()
				.put()
				.then()
				.statusCode(202)
				.body("billInterval", is("MONTHLY"))
				.extract()
				.path("id");
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldEndRecurringBillAndRemoveLaterPayments()
	{
		int billId = createMonthlyBill();

		// payments from January to June remain, the end date is exclusive
		given()
				.pathParam("carId", carId)
				.contentType(ContentType.JSON)
				.body("""
						{ "endDate": "2025-07-01" }
						""")
				.when()
				.post(billId + "/end")
				.then()
				.statusCode(200)
				.body("endDate", is("2025-07-01"))
				.body("total", is(600.0F))
				.body("finished", is(true));

		given()
				.pathParam("carId", carId)
				.queryParam("onlyRunning", true)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("data.size()", is(0));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetOneRecurringBill()
	{
		int billId = createMonthlyBill();

		given()
				.pathParam("carId", carId)
				.when()
				.get(String.valueOf(billId))
				.then()
				.statusCode(200)
				.body("name", is("Insurance"))
				.body("startDate", is("2025-01-01"));

		given()
				.pathParam("carId", carId)
				.when()
				.get("999999")
				.then()
				.statusCode(404);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldRenameRecurringBillAndKeepPayments()
	{
		int billId = createMonthlyBill();
		LocalDate nextDue = LocalDate.parse(given()
				.pathParam("carId", carId)
				.when()
				.get(String.valueOf(billId))
				.then()
				.extract()
				.path("nextDueDate"));

		given()
				.pathParam("carId", carId)
				.contentType(ContentType.JSON)
				.body("""
						{
							"name": "Car insurance",
							"description": "Fully comprehensive",
							"billInterval": "MONTHLY",
							"category": "FINANCE",
							"startDate": "2025-01-01",
							"amount": 100.00
						}
						""")
				.when()
				.post(String.valueOf(billId))
				.then()
				.statusCode(200)
				.body("id", is(billId))
				.body("name", is("Car insurance"))
				.body("description", is("Fully comprehensive"))
				.body("category", is("FINANCE"))
				.body("nextDueDate", is(nextDue.toString()))
				.body("total", is(100.0F * monthsBefore(LocalDate.of(2025, 1, 1), nextDue)));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldBookPaymentsAgainWithChangedSchedule()
	{
		int billId = createMonthlyBill();

		// quarterly from February to the end of the year: February, May, August and
		// November
		given()
				.pathParam("carId", carId)
				.contentType(ContentType.JSON)
				.body("""
						{
							"name": "Insurance",
							"billInterval": "QUARTERLY",
							"category": "INSURANCE",
							"startDate": "2025-02-01",
							"endDate": "2026-01-01",
							"amount": 250.00
						}
						""")
				.when()
				.post(String.valueOf(billId))
				.then()
				.statusCode(200)
				.body("billInterval", is("QUARTERLY"))
				.body("startDate", is("2025-02-01"))
				.body("total", is(1000.0F))
				.body("finished", is(true));

		given()
				.pathParam("carId", carId)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("data.size()", is(1));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldNotUpdateInvalidOrUnknownRecurringBill()
	{
		int billId = createMonthlyBill();
		String invalid = """
				{
					"name": " ",
					"billInterval": "MONTHLY",
					"category": "INSURANCE",
					"startDate": "2025-01-01",
					"amount": 100.00
				}
				""";

		given()
				.pathParam("carId", carId)
				.contentType(ContentType.JSON)
				.body(invalid)
				.when()
				.post(String.valueOf(billId))
				.then()
				.statusCode(400);

		given()
				.pathParam("carId", carId)
				.contentType(ContentType.JSON)
				.body(invalid.replace("\" \"", "\"Insurance\""))
				.when()
				.post("999999")
				.then()
				.statusCode(404);
	}

	/** months with a payment from the start until before the next due date */
	private static int monthsBefore(LocalDate start, LocalDate nextDue)
	{
		return (int) ChronoUnit.MONTHS.between(start, nextDue);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldNotEndRecurringBillBeforeItsStart()
	{
		int billId = createMonthlyBill();

		given()
				.pathParam("carId", carId)
				.contentType(ContentType.JSON)
				.body("""
						{ "endDate": "2024-12-01" }
						""")
				.when()
				.post(billId + "/end")
				.then()
				.statusCode(400);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldNotEndUnknownRecurringBill()
	{
		given()
				.pathParam("carId", carId)
				.contentType(ContentType.JSON)
				.body("""
						{ "endDate": "2025-07-01" }
						""")
				.when()
				.post("999999/end")
				.then()
				.statusCode(404);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldSortRunningAndLatestStartFirstOrByNextDue()
	{
		LocalDate today = LocalDate.now();
		addRecurring("long running", "MONTHLY", today.minusYears(2), null);
		addRecurring("finished", "MONTHLY", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
		addRecurring("new", "MONTHLY", today, today.plusYears(1));
		// created last, but started before "new"
		addRecurring("yearly", "ANNUALLY", today.minusDays(40), null);

		given()
				.pathParam("carId", carId)
				.accept(ContentType.JSON)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("data.name", contains("new", "yearly", "long running", "finished"));

		List<String> nextDue = given()
				.pathParam("carId", carId)
				.queryParam("onlyRunning", true)
				.queryParam("sort", "NEXT_DUE")
				.accept(ContentType.JSON)
				.when()
				.get()
				.then()
				.statusCode(200)
				.extract()
				.jsonPath()
				.getList("data.nextDueDate", String.class);

		// ISO dates sort like strings
		assertEquals(3, nextDue.size());
		assertEquals(nextDue.stream().sorted().toList(), nextDue);
	}

	private void addRecurring(String name, String interval, LocalDate start, LocalDate end)
	{
		String body = """
				{
				    "name": "%s",
				    "billInterval": "%s",
				    "category": "FINANCE",
				    "startDate": "%s",
				    %s
				    "amount": 10.00
				}
				""".formatted(name, interval, start, end == null ? "" : "\"endDate\": \"" + end + "\",");

		given()
				.pathParam("carId", carId)
				.body(body)
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(202);
	}
}
