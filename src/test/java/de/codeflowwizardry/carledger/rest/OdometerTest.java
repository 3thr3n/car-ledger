package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.QueryCounter;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.factory.MaintenanceBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.fuel.FuelBillInput;
import de.codeflowwizardry.carledger.rest.car.maintenance.MaintenanceBillInput;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * The current odometer is the latest reading plus the refuels after it
 */
@QuarkusTest
@TestHTTPEndpoint(CarResource.class)
@TestSecurity(user = "peter", roles = {
		"user"
})
class OdometerTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	FuelBillFactory fuelBillFactory;

	@Inject
	MaintenanceBillFactory maintenanceBillFactory;

	@Inject
	SessionFactory sessionFactory;

	long carId;

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();

		AccountEntity account = new AccountEntity();
		account.setUserId("peter");
		account.setMaxCars(1);
		accountRepository.persist(account);

		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName("Peters car");
		car.setManufactureYear(2020);
		car.setOdometer(50_000);
		car.setOdometerDate(LocalDate.of(2026, 1, 1));
		carRepository.persist(car);
		carId = car.getId();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	private void refuel(LocalDate date, Integer distance)
	{
		fuelBillFactory.create(new FuelBillInput(date, null, 19, distance == null ? null : BigDecimal.valueOf(distance),
				BigDecimal.valueOf(30), BigDecimal.valueOf(180), null), carId, "peter");
	}

	private void maintenance(LocalDate date, int odometer)
	{
		maintenanceBillFactory.create(new MaintenanceBillInput(BigDecimal.TEN, 19, date, odometer, null, null,
				"Service", "Workshop"), carId, "peter");
	}

	@Test
	void shouldAddTheRefuelsAfterTheReading()
	{
		refuel(LocalDate.of(2025, 12, 20), 400);
		// on the day of the reading, it is already included
		refuel(LocalDate.of(2026, 1, 1), 300);
		refuel(LocalDate.of(2026, 2, 1), 600);
		refuel(LocalDate.of(2026, 3, 1), 500);
		refuel(LocalDate.of(2026, 3, 20), null);

		given()
				.when()
				.get("{id}", carId)
				.then()
				.statusCode(200)
				.body("odometer", is(50_000))
				.body("currentOdometer.value", is(51_100))
				.body("currentOdometer.reading", is(50_000))
				.body("currentOdometer.readingDate", is("2026-01-01"))
				.body("currentOdometer.driven", is(1_100));
	}

	@Test
	void shouldStartFromALaterMaintenance()
	{
		refuel(LocalDate.of(2026, 2, 1), 600);
		maintenance(LocalDate.of(2026, 2, 15), 51_000);
		refuel(LocalDate.of(2026, 3, 1), 500);

		given()
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("[0].currentOdometer.value", is(51_500))
				.body("[0].currentOdometer.reading", is(51_000))
				.body("[0].currentOdometer.readingDate", is("2026-02-15"))
				.body("[0].currentOdometer.driven", is(500));
	}

	@Test
	void shouldNeverGoBelowAReading()
	{
		// a later maintenance with a lower odometer, e.g. a typo
		maintenance(LocalDate.of(2026, 3, 10), 49_000);

		given()
				.when()
				.get("{id}", carId)
				.then()
				.statusCode(200)
				.body("currentOdometer.value", is(50_000))
				.body("currentOdometer.reading", is(49_000));
	}

	@Test
	void shouldMakeAChangedOdometerANewReading()
	{
		refuel(LocalDate.of(2026, 2, 1), 600);

		// unchanged odometer, only the name: the reading stays
		given()
				.contentType(ContentType.JSON)
				.body(Map.of("name", "Renamed", "year", 2020, "odometer", 50_000))
				.when()
				.post("{id}", carId)
				.then()
				.statusCode(200)
				.body("odometerDate", is("2026-01-01"))
				.body("currentOdometer.value", is(50_600));

		given()
				.contentType(ContentType.JSON)
				.body(Map.of("name", "Renamed", "year", 2020, "odometer", 52_000))
				.when()
				.post("{id}", carId)
				.then()
				.statusCode(200)
				.body("odometerDate", is(LocalDate.now().toString()))
				.body("currentOdometer.value", is(52_000))
				.body("currentOdometer.driven", is(0));
	}

	@Test
	void shouldNotQueryPerBill()
	{
		Runnable getCars = () -> given().when().get().then().statusCode(200);
		long queriesWithoutBills = QueryCounter.count(sessionFactory, getCars);

		for (int day = 1; day <= 10; day++)
		{
			refuel(LocalDate.of(2026, 4, day), 500);
			maintenance(LocalDate.of(2026, 4, day), 50_000 + day);
		}

		assertEquals(queriesWithoutBills, QueryCounter.count(sessionFactory, getCars));
	}
}
