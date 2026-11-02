package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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
import de.codeflowwizardry.carledger.data.factory.ChargingBillFactory;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.factory.MaintenanceBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.charging.ChargingBillInput;
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
	ChargingBillFactory chargingBillFactory;

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

	private void charge(LocalDate date, int distance)
	{
		chargingBillFactory.create(new ChargingBillInput(date, null, 19, BigDecimal.valueOf(distance),
				BigDecimal.valueOf(10), BigDecimal.valueOf(30), null, null), carId, "peter");
	}

	@Test
	void shouldCountTheDistanceOfAPluginHybridOnce()
	{
		// both are the km since the last refuel or charge, the larger one counts
		refuel(LocalDate.of(2026, 2, 1), 600);
		charge(LocalDate.of(2026, 2, 10), 300);
		charge(LocalDate.of(2026, 3, 1), 400);

		given()
				.when()
				.get("{id}", carId)
				.then()
				.statusCode(200)
				.body("currentOdometer.value", is(50_700))
				.body("currentOdometer.driven", is(700));
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
				// 50,600 at the refuel of February, 500 km since: 100 of them after the
				// maintenance
				.body("[0].currentOdometer.value", is(51_100))
				.body("[0].currentOdometer.reading", is(51_000))
				.body("[0].currentOdometer.readingDate", is("2026-02-15"))
				.body("[0].currentOdometer.driven", is(100));
	}

	@Test
	void shouldNotCountTheDistanceBeforeAReadingTwice()
	{
		// at 50,600 km after the refuel of February
		refuel(LocalDate.of(2026, 2, 1), 600);
		// 300 km later a reading, e.g. the end of a trip
		maintenance(LocalDate.of(2026, 2, 15), 50_900);
		// 600 km since the refuel of February: at 51,200 km, 300 of them before the
		// reading
		refuel(LocalDate.of(2026, 3, 1), 600);

		given()
				.when()
				.get("{id}", carId)
				.then()
				.statusCode(200)
				.body("currentOdometer.value", is(51_200));
	}

	@Test
	void shouldCountThePartAfterTheReadingByTheDays()
	{
		// without its distance, the odometer at this refuel is unknown
		refuel(LocalDate.of(2026, 2, 1), null);
		maintenance(LocalDate.of(2026, 2, 15), 50_900);
		// 600 km in 28 days, the reading after 14 of them: 300 km count
		refuel(LocalDate.of(2026, 3, 1), 600);

		given()
				.when()
				.get("{id}", carId)
				.then()
				.statusCode(200)
				.body("currentOdometer.value", is(51_200))
				.body("currentOdometer.driven", is(300));
	}

	@Test
	void shouldEstimateTheOdometerOfToday()
	{
		LocalDate today = LocalDate.now();
		// the start of the average, about 90 days before the latest data
		maintenance(today.minusDays(95), 51_000);
		// 2,550 km in the 85 days up to the refuel: 30 a day
		refuel(today.minusDays(10), 2_550);

		given()
				.when()
				.get("{id}", carId)
				.then()
				.statusCode(200)
				.body("currentOdometer.value", is(53_550))
				.body("currentOdometer.perDay", is(30.0f))
				// 10 days since the refuel
				.body("currentOdometer.estimatedToday", is(53_850));
	}

	@Test
	void shouldStopTheEstimateAfter60Days()
	{
		LocalDate today = LocalDate.now();
		maintenance(today.minusDays(250), 51_000);
		// 3,000 km in 100 days, 150 days ago: only 60 of them count
		maintenance(today.minusDays(150), 54_000);

		given()
				.when()
				.get("{id}", carId)
				.then()
				.statusCode(200)
				.body("currentOdometer.value", is(54_000))
				.body("currentOdometer.perDay", is(30.0f))
				.body("currentOdometer.estimatedToday", is(55_800));
	}

	@Test
	void shouldNotEstimateWithoutAnOlderReading()
	{
		// the reading of the car becomes today's
		given()
				.contentType(ContentType.JSON)
				.body(Map.of("name", "Peters car", "year", 2020, "odometer", 52_000))
				.when()
				.post("{id}", carId)
				.then()
				.statusCode(200)
				.body("currentOdometer.value", is(52_000))
				.body("currentOdometer.perDay", nullValue())
				.body("currentOdometer.estimatedToday", nullValue());
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
