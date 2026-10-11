package de.codeflowwizardry.carledger.rest.car.fuel;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.QueryCounter;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestHTTPEndpoint(FuelResource.class)
public class FuelResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	FuelBillFactory fuelBillFactory;

	@Inject
	SessionFactory sessionFactory;

	CarEntity carEntity;

	@BeforeEach
	@Transactional
	void setup()
	{
		setupBob();
		setupPeter();
	}

	private void setupPeter()
	{
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setMaxCars(1);
		accountEntity.setUserId("peter");
		accountRepository.persist(accountEntity);

		carEntity = new CarEntity();
		carEntity.setUser(accountEntity);
		carEntity.setName("Neat car");
		carRepository.persist(carEntity);

		FuelBillInput fuelBillInput = new FuelBillInput(
				LocalDate.of(2024, 8, 16),
				BigDecimal.ZERO,
				19,
				BigDecimal.valueOf(500),
				BigDecimal.valueOf(28d),
				BigDecimal.valueOf(199.9d),
				BigDecimal.valueOf(8.5));

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());

		fuelBillInput = new FuelBillInput(
				LocalDate.of(2022, 5, 22),
				BigDecimal.ZERO,
				19,
				BigDecimal.valueOf(400),
				BigDecimal.valueOf(20d),
				BigDecimal.valueOf(189.9d),
				BigDecimal.valueOf(9.1));

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());

		fuelBillInput = new FuelBillInput(
				LocalDate.of(2023, 6, 2),
				BigDecimal.ZERO,
				19,
				BigDecimal.valueOf(480),
				BigDecimal.valueOf(28d),
				BigDecimal.valueOf(196.9d),
				BigDecimal.valueOf(8.2));

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());
	}

	private void setupBob()
	{
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setMaxCars(1);
		accountEntity.setUserId("bob");
		accountRepository.persist(accountEntity);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllMyBillsInOrder()
	{
		given()
				.pathParam("carId", carEntity.getId())
				.when()
				.get("all")
				.then()
				.statusCode(200)
				.body("total", is(3))
				.body("page", is(1))
				.body("size", is(10))
				.body("data.size()", is(3))
				.body("data[0].distance", is(500.00F))
				.body("data[0].date", is("2024-08-16"))
				.body("data[1].distance", is(480.00F))
				.body("data[1].date", is("2023-06-02"))
				.body("data[2].distance", is(400.00F))
				.body("data[2].date", is("2022-05-22"));

		given()
				.queryParam("page", 0)
				.queryParam("size", 1)
				.pathParam("carId", carEntity.getId())
				.when()
				.get("all")
				.then()
				.statusCode(200)
				.body("total", is(3))
				.body("page", is(1))
				.body("size", is(1))
				.body("data.size()", is(1))
				.body("data[0].distance", is(500.00F))
				.body("data[0].date", is("2024-08-16"));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldNotQueryPerBill()
	{
		Runnable getBills = () -> given()
				.pathParam("carId", carEntity.getId())
				.queryParam("size", 100)
				.when()
				.get("all")
				.then()
				.statusCode(200);

		long queriesWithThreeBills = QueryCounter.count(sessionFactory, getBills);

		for (int day = 1; day <= 10; day++)
		{
			fuelBillFactory.create(new FuelBillInput(LocalDate.of(2020, 1, day), null, 19, BigDecimal.valueOf(400),
					BigDecimal.valueOf(30), BigDecimal.valueOf(180), null), carEntity.getId(), "peter");
		}

		assertEquals(queriesWithThreeBills, QueryCounter.count(sessionFactory, getBills));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldLimitPageSize()
	{
		given()
				.pathParam("carId", carEntity.getId())
				.queryParam("size", 100000)
				.when()
				.get("all")
				.then()
				.statusCode(200)
				.body("size", is(100));

		given()
				.pathParam("carId", carEntity.getId())
				.queryParam("size", 0)
				.when()
				.get("all")
				.then()
				.statusCode(200)
				.body("size", is(1))
				.body("data.size()", is(1));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldAddBill()
	{
		// given
		String body = """
				{
					"date": "2024-08-22",
					"distance": 450,
					"unit": 30,
					"pricePerUnit": 188.9,
					"estimateConsumption": 8.9,
					"vatRate": 19
				}
				""";

		// when
		given()
				.pathParam("carId", carEntity.getId())
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(body)
				.put()
				.then()
				.statusCode(202)
				.body("date", is("2024-08-22"));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldAddBillWithoutPricePerUnitAndOptionalValues()
	{
		// given
		String body = """
				{
					"date": "2024-09-01",
					"unit": 25,
					"total": 50,
					"vatRate": 19
				}
				""";

		// when
		given()
				.pathParam("carId", carEntity.getId())
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(body)
				.put()
				.then()
				.statusCode(202)
				.body("pricePerUnit", is(200.00F))
				.body("estimateConsumption", is(0.00F))
				.body("distance", is(0.00F));

		// then
		given()
				.pathParam("carId", carEntity.getId())
				.queryParam("year", 2024)
				.when()
				.get("all")
				.then()
				.statusCode(200)
				.body("total", is(2))
				.body("data[0].date", is("2024-09-01"))
				.body("data[0].pricePerUnit", is(200.00F));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldRejectBillWithOnlyOneValueGreaterThanZero()
	{
		// given
		String body = """
				{
					"date": "2024-09-01",
					"unit": 0,
					"total": 50,
					"vatRate": 19
				}
				""";

		// when
		given()
				.pathParam("carId", carEntity.getId())
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(body)
				.put()
				.then()
				.statusCode(400);
	}

	@Test
	@TestSecurity(user = "bob", roles = {
			"user"
	})
	void shouldFailAddingBillAsDifferentUser()
	{
		// given
		String body = """
				{
					"date": "2024-08-22",
					"distance": 450,
					"unit": 30,
					"pricePerUnit": 188.9,
					"estimateConsumption": 8.9,
					"vatRate": 19
				}
				""";

		// when
		given()
				.pathParam("carId", carEntity.getId())
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(body)
				.put()
				.then()
				.statusCode(404);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetYearsOfBills()
	{
		// when
		given()
				.pathParam("carId", carEntity.getId())
				.when()
				.get("years")
				.then()
				.statusCode(200)
				.body("", contains(2024, 2023, 2022));
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}
}
