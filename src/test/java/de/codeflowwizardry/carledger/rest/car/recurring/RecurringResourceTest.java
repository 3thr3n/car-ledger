package de.codeflowwizardry.carledger.rest.car.recurring;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

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
				.body("size()", is(0));
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

		given()
				.pathParam("carId", 99)
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(409);
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
				.statusCode(400);
	}
}
