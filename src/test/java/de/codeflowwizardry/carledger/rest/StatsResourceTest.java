package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.fuel.FuelBillInput;
import de.codeflowwizardry.carledger.rest.car.stats.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestHTTPEndpoint(StatsResource.class)
class StatsResourceTest
{
	@Inject
	FuelBillFactory fuelBillFactory;

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	CarEntity carEntity;

	@BeforeEach
	@Transactional
	void setup()
	{
		cleanup();
		setupPeter();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
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

		// 55,972
		// 5.6
		FuelBillInput fuelBillInput = new FuelBillInput(
				LocalDate.now(),
				BigDecimal.ZERO,
				BigInteger.valueOf(19),
				BigDecimal.valueOf(500),
				BigDecimal.valueOf(28d),
				BigDecimal.valueOf(199.9d),
				BigDecimal.valueOf(8.5));

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());

		// 37,98
		// 5.0
		fuelBillInput = new FuelBillInput(
				LocalDate.of(2022, 5, 22),
				BigDecimal.ZERO,
				BigInteger.valueOf(19),
				BigDecimal.valueOf(400),
				BigDecimal.valueOf(20d),
				BigDecimal.valueOf(189.9d),
				BigDecimal.valueOf(9.1));

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());

		// 55,132
		// 5.83
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

	DashboardStats expected = new DashboardStats(
			new TotalStats(
					BigDecimal.valueOf(76.0).setScale(2, RoundingMode.HALF_UP),
					BigDecimal.valueOf(1380.0).setScale(2, RoundingMode.HALF_UP),
					BigDecimal.valueOf(149.08),
					BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP),
					BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP),
					BigDecimal.valueOf(149.08),
					3,
					0,
					0),
			new AverageStats(
					BigDecimal.valueOf(195.6),
					BigDecimal.valueOf(460.00).setScale(2, RoundingMode.HALF_UP),
					BigDecimal.valueOf(10.73),
					BigDecimal.valueOf(5.48),
					BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP),
					BigDecimal.valueOf(10.80).setScale(2, RoundingMode.HALF_UP)),
			new HiLoStats(
					new HiLo(
							BigDecimal.valueOf(189.9).setScale(1, RoundingMode.HALF_UP),
							BigDecimal.valueOf(199.9).setScale(1, RoundingMode.HALF_UP),
							0),
					new HiLo(
							BigDecimal.valueOf(400.00).setScale(2, RoundingMode.HALF_UP),
							BigDecimal.valueOf(500.00).setScale(2, RoundingMode.HALF_UP),
							0),
					new HiLo(
							BigDecimal.valueOf(9.50).setScale(2, RoundingMode.HALF_UP),
							BigDecimal.valueOf(11.49).setScale(2, RoundingMode.HALF_UP),
							0),
					new HiLo(
							BigDecimal.valueOf(5.00).setScale(2, RoundingMode.HALF_UP),
							BigDecimal.valueOf(5.83).setScale(2, RoundingMode.HALF_UP),
							0),
					new HiLo(
							BigDecimal.valueOf(37.98).setScale(2, RoundingMode.HALF_UP),
							BigDecimal.valueOf(55.97).setScale(2, RoundingMode.HALF_UP),
							0),
					new HiLo(
							BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP),
							BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP),
							0)));

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllTotalStats()
	{
		DashboardStats stats = given()
				.pathParam("carId", carEntity.getId())
				.when()
				.get()
				.then()
				.statusCode(200)
				.extract()
				.as(DashboardStats.class);

		Assertions.assertEquals(expected, stats);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllTotalStatsForEveryOfMyCars()
	{
		DashboardStats stats = given()
				.pathParam("carId", -1)
				.when()
				.get()
				.then()
				.statusCode(200)
				.extract()
				.as(DashboardStats.class);

		Assertions.assertEquals(expected, stats);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllTotalStatsAfter2022()
	{
		String localDateString = LocalDate.of(2023, 1, 1).atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE);

		given()
				.pathParam("carId", carEntity.getId())
				.queryParam("from", localDateString)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("total.trackedDistance", is(980.00f))
				.body("total.unit", is(56.00f))
				.body("total.fuelTotal", is(111.10f));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllTotalStatsOf2023()
	{
		String fromLocalDate = LocalDate.of(2023, 1, 1).atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE);
		String toLocalDate = LocalDate.of(2023, 12, 31).atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE);

		given()
				.pathParam("carId", carEntity.getId())
				.queryParam("from", fromLocalDate)
				.queryParam("to", toLocalDate)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("total.trackedDistance", is(480.00f))
				.body("total.unit", is(28.00f))
				.body("total.fuelTotal", is(55.13f));
	}
}
