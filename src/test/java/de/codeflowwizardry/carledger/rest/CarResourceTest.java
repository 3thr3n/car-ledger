package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.Car;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class CarResourceTest extends AbstractResourceTest
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
		Account account = accountRepository.findByIdentifier("bob");
		account.setMaxCars(2);
		accountRepository.persist(account);

		Car car = new Car();
		car.setUser(account);
		car.setDescription("Normal car");
		carRepository.persist(car);

		carId = car.getId();
	}

	@Test
	void shouldGetMyCars()
	{
		given()
				.cookie(bobCookie)
				.when()
				.get("/api/car/my")
				.then()
				.statusCode(200)
				.body("size()", Matchers.equalTo(1));
	}

	@Test
	void shouldGetFailGettingCar()
	{
		given()
				.cookie(bobCookie)
				.when()
				.pathParam("id", 1)
				.get("/api/car/my/{id}")
				.then()
				.statusCode(204);
	}

	@Test
	void shouldGetMySpecificCar()
	{
		given()
				.cookie(bobCookie)
				.when()
				.pathParam("id", carId)
				.get("/api/car/my/{id}")
				.then()
				.statusCode(200)
				.body("id", Matchers.equalTo((int) carId))
				.body("amountBills", Matchers.equalTo(0));
	}

	@Test
	@Disabled
	void shouldFailGettingCarData()
	{
		given()
				.cookie(bobCookie)
				.when()
				.get("/api/car/my")
				.then()
				.statusCode(400);
	}

	@Test
	void shouldCreateACar()
	{
		String body = "{\"description\": \"Hansi\"}";
		given()
				.cookie(bobCookie)
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.put("/api/car/my")
				.then()
				.statusCode(202);
	}

	@Test
	void shouldFailCreateCarIfAlreadyAtMaxCapacity()
	{
		String body = "{\"description\": \"Hansi\"}";
		given()
				.cookie(bobCookie)
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.put("/api/car/my")
				.then()
				.statusCode(202);

		body = "{\"description\": \"Hansi 2\"}";

		given()
				.cookie(bobCookie)
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.put("/api/car/my")
				.then()
				.statusCode(400);
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		carRepository.deleteAll();
		accountRepository.deleteAll();
	}
}
