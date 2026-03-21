package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.fuel.FuelBillInput;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestHTTPEndpoint(CarResource.class)
class CarResourceTest
{
	@Inject
	FuelBillFactory fuelBillFactory;

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	long carId = 0;

	@BeforeEach
	@Transactional
	void before()
	{
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setMaxCars(2);
		accountEntity.setUserId("peter");
		accountRepository.persist(accountEntity);

		CarEntity carEntity = new CarEntity();
		carEntity.setUser(accountEntity);
		carEntity.setName("Normal car");
		carRepository.persist(carEntity);

		FuelBillInput fuelBillInput = new FuelBillInput(
				LocalDate.now(),
				BigDecimal.ZERO,
				BigInteger.valueOf(19),
				BigDecimal.valueOf(500),
				BigDecimal.valueOf(40d),
				BigDecimal.valueOf(199.9d),
				BigDecimal.valueOf(8.0));

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());
		carId = carEntity.getId();
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetMyCars()
	{
		given()
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("size()", Matchers.equalTo(1));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetMyCarOverview()
	{
		given()
				.when()
				.pathParam("id", carId)
				.get("{id}/overview")
				.then()
				.statusCode(200)
				.body("totalRefuels", is(1))
				.body("totalCost", is(79.96F))
				.body("avgConsumption", is(8.0F));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetFailGettingCar()
	{
		given()
				.when()
				.pathParam("id", 1)
				.get("{id}")
				.then()
				.statusCode(204);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetMySpecificCar()
	{
		given()
				.when()
				.pathParam("id", carId)
				.get("{id}")
				.then()
				.statusCode(200)
				.body("id", Matchers.equalTo((int) carId))
				.body("amountBills", Matchers.equalTo(1));
	}

	@Test
	@TestSecurity(user = "bob", roles = {
			"user"
	})
	void shouldFailGettingCarData()
	{
		given().when().get().then().statusCode(400);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldCreateACar()
	{
		String body = "{\"name\": \"Hansi\", \"year\": \"2000\"}";
		given()
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.put()
				.then()
				.statusCode(201);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldFailCreateCarIfAlreadyAtMaxCapacity()
	{
		String body = "{\"name\": \"Hansi\", \"year\": \"2000\"}";
		given()
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.put()
				.then()
				.statusCode(201);

		body = "{\"name\": \"Hansi 2\", \"year\": \"2010\"}";

		given()
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.put()
				.then()
				.statusCode(400);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldNotCreateCarWithInvalidBody()
	{
		String body = "{\"namxe\": \"Hans\", \"year\": \"2000\"}";
		given()
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.put()
				.then()
				.statusCode(400);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldUpdateMyCar()
	{
		String body = "{\"name\": \"Hans\", \"year\": \"2000\"}";
		given()
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.post("{id}", carId)
				.then()
				.statusCode(200);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldNotUpdateMyCarWithInvalidBody()
	{
		String body = "{\"name\": \"Hans\", \"years\": \"2000\"}";
		given()
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.post("{id}", carId)
				.then()
				.statusCode(400);
	}

	@Test
	void shouldBeRedirectToLoginUrl()
	{
		String response = given()
				.when()
				.get()
				.then()
				.statusCode(200)
				.extract()
				.response()
				.getBody()
				.asString();

		assertTrue(response.contains("<html"));
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}
}
