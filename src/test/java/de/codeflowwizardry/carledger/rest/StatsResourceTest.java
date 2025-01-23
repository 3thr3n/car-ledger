package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.Bill;
import de.codeflowwizardry.carledger.data.Car;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
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
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	BillRepository billRepository;

	Car car;

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
		Account account = new Account();
		account.setMaxCars(1);
		account.setUserId("peter");
		accountRepository.persist(account);

		car = new Car();
		car.setUser(account);
		car.setDescription("Neat car");
		carRepository.persist(car);

		Bill bill = new Bill();
		bill.setEstimate(BigDecimal.valueOf(8.5));
		bill.setDay(LocalDate.now());
		bill.setDistance(BigDecimal.valueOf(500));
		bill.setUnit(BigDecimal.valueOf(28d));
		bill.setPricePerUnit(BigDecimal.valueOf(199.9d));
		// 55,972
		// 5.6
		bill.setCar(car);
		billRepository.persist(bill);

		bill = new Bill();
		bill.setEstimate(BigDecimal.valueOf(9.1d));
		bill.setDay(LocalDate.of(2022, 5, 22));
		bill.setDistance(BigDecimal.valueOf(400));
		bill.setUnit(BigDecimal.valueOf(20d));
		bill.setPricePerUnit(BigDecimal.valueOf(189.9d));
		// 37,98
		// 5.0
		bill.setCar(car);
		billRepository.persist(bill);

		bill = new Bill();
		bill.setEstimate(BigDecimal.valueOf(8.2d));
		bill.setDay(LocalDate.of(2023, 6, 2));
		bill.setDistance(BigDecimal.valueOf(480));
		bill.setUnit(BigDecimal.valueOf(28d));
		bill.setPricePerUnit(BigDecimal.valueOf(196.9d));
		// 55,132
		// 5.83
		bill.setCar(car);
		billRepository.persist(bill);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllTotalStats()
	{
		given()
				.pathParam("carId", car.getId())
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
				.pathParam("carId", car.getId())
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
				.pathParam("carId", car.getId())
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
				.pathParam("carId", car.getId())
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
				.pathParam("carId", car.getId())
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
