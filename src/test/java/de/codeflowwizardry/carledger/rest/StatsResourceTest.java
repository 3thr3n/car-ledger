package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.records.FuelBillInput;
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

	@Inject
	BillRepository billRepository;

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
		billRepository.deleteAll();
		carRepository.deleteAll();
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
				BigDecimal.valueOf(500),
				BigDecimal.valueOf(28d),
				BigDecimal.valueOf(199.9d),
				BigDecimal.valueOf(8.5),
				BigInteger.valueOf(19),
				BigDecimal.ZERO);

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());

		// 37,98
		// 5.0
		fuelBillInput = new FuelBillInput(
				LocalDate.of(2022, 5, 22),
				BigDecimal.valueOf(400),
				BigDecimal.valueOf(20d),
				BigDecimal.valueOf(189.9d),
				BigDecimal.valueOf(9.1),
				BigInteger.valueOf(19),
				BigDecimal.ZERO);

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());

		// 55,132
		// 5.83
		fuelBillInput = new FuelBillInput(
				LocalDate.of(2023, 6, 2),
				BigDecimal.valueOf(480),
				BigDecimal.valueOf(28d),
				BigDecimal.valueOf(196.9d),
				BigDecimal.valueOf(8.2),
				BigInteger.valueOf(19),
				BigDecimal.ZERO);

		fuelBillFactory.create(fuelBillInput, carEntity.getId(), carEntity.getUser().getUserId());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllTotalStats()
	{
		given()
				.pathParam("carId", carEntity.getId())
				.when()
				.get("/total")
				.then()
				.statusCode(200)
				.body("distance", is("1380.00"))
				.body("unit", is("76.00"))
				.body("calculatedPrice", is("149.08"));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllTotalStatsForEveryOfMyCars()
	{
		given()
				.pathParam("carId", -1)
				.when()
				.get("/total")
				.then()
				.statusCode(200)
				.body("distance", is("1380.00"))
				.body("unit", is("76.00"))
				.body("calculatedPrice", is("149.08"));
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
				.get("/total")
				.then()
				.statusCode(200)
				.body("distance", is("980.00"))
				.body("unit", is("56.00"))
				.body("calculatedPrice", is("111.10"));
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
				.get("/total")
				.then()
				.statusCode(200)
				.body("distance", is("480.00"))
				.body("unit", is("28.00"))
				.body("calculatedPrice", is("55.13"));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllAverageStats()
	{
		given()
				.pathParam("carId", carEntity.getId())
				.when()
				.get("/average")
				.then()
				.statusCode(200)
				.body("pricePerUnit", is("195.6"))
				.body("distance", is("460.00"))
				.body("calculated", is("5.48"))
				.body("calculatedPrice", is("49.69"));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllHiLoStats()
	{
		given()
				.pathParam("carId", carEntity.getId())
				.when()
				.get("/hi_lo")
				.then()
				.statusCode(200)
				.body("distance.max", is("500.00"))
				.body("distance.min", is("400.00"))
				.body("unit.max", is("28.00"))
				.body("unit.min", is("20.00"))
				.body("calculatedPrice.max", is("55.97"))
				.body("calculatedPrice.min", is("37.98"))
				.body("calculated.max", is("5.83"))
				.body("calculated.min", is("5.00"))
				.body("pricePerUnit.max", is("199.9"))
				.body("pricePerUnit.min", is("189.9"));
	}
}
