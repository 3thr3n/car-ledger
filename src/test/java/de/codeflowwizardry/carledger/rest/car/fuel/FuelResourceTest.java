package de.codeflowwizardry.carledger.rest.car.fuel;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.records.input.FuelBillInput;
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
	BillRepository billRepository;

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	FuelBillFactory fuelBillFactory;

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
				BigInteger.valueOf(19),
				BigDecimal.valueOf(500),
				BigDecimal.valueOf(28d),
				BigDecimal.valueOf(199.9d),
				BigDecimal.valueOf(8.5));

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());

		fuelBillInput = new FuelBillInput(
				LocalDate.of(2022, 5, 22),
				BigDecimal.ZERO,
				BigInteger.valueOf(19),
				BigDecimal.valueOf(400),
				BigDecimal.valueOf(20d),
				BigDecimal.valueOf(189.9d),
				BigDecimal.valueOf(9.1));

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());

		fuelBillInput = new FuelBillInput(
				LocalDate.of(2023, 6, 2),
				BigDecimal.ZERO,
				BigInteger.valueOf(19),
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
				.body("data[0].distance", is("500.00"))
				.body("data[0].date", is("2024-08-16"))
				.body("data[1].distance", is("480.00"))
				.body("data[1].date", is("2023-06-02"))
				.body("data[2].distance", is("400.00"))
				.body("data[2].date", is("2022-05-22"));
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
					"estimate": 8.9,
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
					"estimate": 8.9,
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
	void shouldBeRedirectToLoginUrl()
	{
		String response = given()
				.pathParam("carId", carEntity.getId())
				.when()
				.get("all")
				.then()
				.statusCode(200).extract()
				.response()
				.getBody()
				.asString();

		assertTrue(response.contains("<html"));
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
		billRepository.deleteAll();
		carRepository.deleteAll();
		accountRepository.deleteAll();
	}
}
