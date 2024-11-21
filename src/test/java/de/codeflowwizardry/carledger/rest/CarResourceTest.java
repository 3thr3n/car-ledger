package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.Car;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
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
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	long carId = 0;

	@BeforeEach
	@Transactional
	void before()
	{
		Account account = new Account();
		account.setMaxCars(2);
		account.setUserId("peter");
		accountRepository.persist(account);

		Car car = new Car();
		car.setUser(account);
		car.setDescription("Normal car");
		carRepository.persist(car);

		carId = car.getId();
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetMyCars()
	{
		given().when().get().then().statusCode(200).body("size()", Matchers.equalTo(1));
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
				.body("amountBills", Matchers.equalTo(0));
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
		String body = "{\"description\": \"Hansi\"}";
		given()
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.put("/api/car/my")
				.then()
				.statusCode(202);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldFailCreateCarIfAlreadyAtMaxCapacity()
	{
		String body = "{\"description\": \"Hansi\"}";
		given()
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.put("/api/car/my")
				.then()
				.statusCode(202);

		body = "{\"description\": \"Hansi 2\"}";

		given()
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.put("/api/car/my")
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
		carRepository.deleteAll();
		accountRepository.deleteAll();
	}
}
