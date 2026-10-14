package de.codeflowwizardry.carledger.rest.car.charging;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.ChargingLocation;
import de.codeflowwizardry.carledger.data.EnergyType;
import de.codeflowwizardry.carledger.data.factory.ChargingBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestHTTPEndpoint(ChargingResource.class)
class ChargingResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	ChargingBillFactory chargingBillFactory;

	long carId;

	long billId;

	@BeforeEach
	@Transactional
	void setup()
	{
		AccountEntity bob = new AccountEntity();
		bob.setMaxCars(1);
		bob.setUserId("bob");
		accountRepository.persist(bob);

		AccountEntity account = new AccountEntity();
		account.setMaxCars(1);
		account.setUserId("peter");
		accountRepository.persist(account);

		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName("Electric car");
		car.setEnergyType(EnergyType.ELECTRIC);
		carRepository.persist(car);
		carId = car.getId();

		// 40 kWh for 30 ct, 250 km: 16 kWh/100km
		billId = chargingBillFactory.create(new ChargingBillInput(LocalDate.of(2026, 3, 1), null, 19,
				BigDecimal.valueOf(250), BigDecimal.valueOf(40), BigDecimal.valueOf(30), ChargingLocation.HOME, 80),
				carId, "peter").getBill().getId();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldCalculateTheMissingValueAndTheConsumption()
	{
		given()
				.pathParam("carId", carId)
				.when()
				.get("{billId}", billId)
				.then()
				.statusCode(200)
				.body("total", is(12.0F))
				.body("unit", is(40.0F))
				.body("pricePerUnit", is(30.0F))
				.body("avgConsumption", is(16.0F))
				.body("location", is("HOME"))
				.body("stateOfCharge", is(80));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldAddAPublicChargeWithoutKwh()
	{
		given()
				.pathParam("carId", carId)
				.contentType(ContentType.JSON)
				.body("""
						{ "date": "2026-04-02", "vatRate": 19, "total": 27.3, "pricePerUnit": 59.0,
						  "location": "PUBLIC" }
						""")
				.when()
				.put()
				.then()
				.statusCode(202)
				.body("unit", is(46.27F))
				.body("avgConsumption", is(0.0F))
				.body("location", is("PUBLIC"))
				.body("stateOfCharge", nullValue());

		given()
				.pathParam("carId", carId)
				.when()
				.get("all")
				.then()
				.statusCode(200)
				.body("data.date", contains("2026-04-02", "2026-03-01"));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldRejectInvalidCharges()
	{
		given()
				.pathParam("carId", carId)
				.contentType(ContentType.JSON)
				.body("""
						{ "date": "2026-04-02", "vatRate": 19, "unit": 30, "stateOfCharge": 101 }
						""")
				.when()
				.put()
				.then()
				.statusCode(400)
				.body("size()", is(2));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldUpdateTheChargeAndCalculateAgain()
	{
		given()
				.pathParam("carId", carId)
				.contentType(ContentType.JSON)
				.body("""
						{ "date": "2026-03-01", "vatRate": 19, "unit": 40, "pricePerUnit": 30, "distance": 200 }
						""")
				.when()
				.post("{billId}", billId)
				.then()
				.statusCode(200)
				.body("avgConsumption", is(20.0F))
				.body("location", nullValue())
				.body("stateOfCharge", nullValue());
	}

	@Test
	@TestSecurity(user = "bob", roles = {
			"user"
	})
	void shouldNotGetTheChargeOfOtherUsers()
	{
		given()
				.pathParam("carId", carId)
				.when()
				.get("{billId}", billId)
				.then()
				.statusCode(404);
	}
}
