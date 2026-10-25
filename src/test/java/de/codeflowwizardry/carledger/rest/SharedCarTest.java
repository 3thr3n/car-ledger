package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarAccessEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.CarRole;
import de.codeflowwizardry.carledger.data.factory.MiscellaneousBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccessRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.miscellaneous.MiscellaneousBillInput;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * A car of peter shared with paul: what paul sees and may do by his role
 */
@QuarkusTest
@TestSecurity(user = "paul", roles = "user")
class SharedCarTest
{
	private static final Map<String, Object> BILL = Map.of("total", 12, "vatRate", 19, "date", "2026-10-01",
			"description", "Car wash");

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	CarAccessRepository carAccessRepository;

	@Inject
	EntityManager entityManager;

	@Inject
	MiscellaneousBillFactory miscellaneousBillFactory;

	long carId;

	@BeforeEach
	void setup()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			AccountEntity peter = account("peter");
			account("paul");

			CarEntity car = new CarEntity();
			car.setUser(peter);
			car.setName("Golf");
			car.setManufactureYear(2020);
			car.setPurchaseDate(LocalDate.of(2020, 5, 1));
			car.setPurchasePrice(BigDecimal.valueOf(25_000));
			carRepository.persist(car);
			carId = car.getId();
		});
	}

	@AfterEach
	void cleanup()
	{
		QuarkusTransaction.requiringNew().run(() -> accountRepository.deleteAll());
	}

	private AccountEntity account(String name)
	{
		AccountEntity account = new AccountEntity();
		account.setUserId(name);
		account.setMaxCars(1);
		accountRepository.persist(account);
		return account;
	}

	private void share(CarRole role)
	{
		QuarkusTransaction.requiringNew().run(() -> carAccessRepository.persist(new CarAccessEntity(
				entityManager.find(CarEntity.class, carId), accountRepository.find("userId", "paul").firstResult(),
				role, null)));
	}

	@Test
	void shouldShowNothingWithoutAnAccess()
	{
		given().get("/api/car/my").then().statusCode(200).body("size()", is(0));
		given().get("/api/car/my/{id}", carId).then().statusCode(404);
	}

	@Test
	void shouldShowTheCarWithTheRoleOfAViewer()
	{
		share(CarRole.VIEWER);

		given().get("/api/car/my").then().statusCode(200)
				.body("size()", is(1))
				.body("[0].role", is("VIEWER"))
				.body("[0].purchasePrice", is(25_000.0F))
				.body("[0].currentOdometer", notNullValue());
		given().get("/api/car/my/{id}", carId).then().statusCode(200).body("role", is("VIEWER"));
		given().get("/api/bill/{carId}/miscellaneous/all", carId).then().statusCode(200);

		given().contentType(ContentType.JSON).body(BILL)
				.put("/api/bill/{carId}/miscellaneous", carId).then().statusCode(404);
	}

	@Test
	void shouldLetAnEditorAddEntriesButNotChangeTheCar()
	{
		share(CarRole.EDITOR);

		given().contentType(ContentType.JSON).body(BILL)
				.put("/api/bill/{carId}/miscellaneous", carId).then().statusCode(anyOf(is(200), is(201), is(202)));
		given().get("/api/bill/{carId}/miscellaneous/all", carId).then().statusCode(200).body("total", is(1));

		given().delete("/api/car/my/{id}", carId).then().statusCode(404);
	}

	@Test
	void shouldShowADriverOnlyTheCar()
	{
		share(CarRole.DRIVER);
		QuarkusTransaction.requiringNew().run(() -> miscellaneousBillFactory.create(
				new MiscellaneousBillInput(BigDecimal.TEN, 19, LocalDate.of(2026, 10, 1), "Parking"), carId, "peter"));

		given().get("/api/car/my").then().statusCode(200)
				.body("size()", is(1))
				.body("[0].role", is("DRIVER"))
				.body("[0].name", is("Golf"))
				// the odometer, for the trips
				.body("[0].currentOdometer", notNullValue())
				// not the purchase
				.body("[0].purchasePrice", nullValue())
				.body("[0].purchaseDate", nullValue());
		// the entries of the owner aren't for the driver
		given().get("/api/bill/{carId}/miscellaneous/all", carId).then().statusCode(200)
				.body("total", is(0))
				.body("data", empty());
		given().get("/api/car/my/{id}/overview", carId).then().statusCode(404);
		given().contentType(ContentType.JSON).body(BILL)
				.put("/api/bill/{carId}/miscellaneous", carId).then().statusCode(404);
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldMarkTheOwnCar()
	{
		given().get("/api/car/my").then().statusCode(200).body("[0].role", is("OWNER"));
	}

	@Test
	void shouldCountOnlyTheOwnCarsForTheLimit()
	{
		share(CarRole.EDITOR);

		given().contentType(ContentType.JSON)
				.body(Map.of("name", "Polo", "year", 2021, "odometer", 1000))
				.put("/api/car/my").then().statusCode(201);
		given().get("/api/car/my").then().statusCode(200)
				.body("size()", is(2))
				.body("role", containsInAnyOrder("OWNER", "EDITOR"));
	}
}
