package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.BillCategory;
import de.codeflowwizardry.carledger.data.BillInterval;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.RecurringBillEntity;
import de.codeflowwizardry.carledger.data.RecurringBillPaymentEntity;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillPaymentRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillRepository;
import de.codeflowwizardry.carledger.rest.car.fuel.FuelBillInput;
import io.quarkus.narayana.jta.QuarkusTransaction;
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

	@Inject
	BillRepository billRepository;

	@Inject
	RecurringBillRepository recurringBillRepository;

	@Inject
	RecurringBillPaymentRepository recurringBillPaymentRepository;

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
				19,
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
	void shouldAddRecurringPaymentsToTheTotalCostOfTheOverview()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			RecurringBillEntity insurance = new RecurringBillEntity(carRepository.findById(carId, "peter"));
			insurance.setName("Insurance");
			insurance.setCategory(BillCategory.FINANCE);
			insurance.setInterval(BillInterval.MONTHLY);
			insurance.setAmount(BigDecimal.valueOf(40));
			insurance.setStartDate(LocalDate.now().minusMonths(1));
			insurance.setNextDueDate(LocalDate.now().plusMonths(1));
			insurance.setTotal(BigDecimal.valueOf(80));
			recurringBillRepository.persist(insurance);

			for (int month = 0; month < 2; month++)
			{
				RecurringBillPaymentEntity payment = new RecurringBillPaymentEntity(insurance);
				payment.setDueDate(LocalDate.now().minusMonths(month));
				payment.setPaidDate(payment.getDueDate());
				payment.setAmount(BigDecimal.valueOf(40));
				recurringBillPaymentRepository.persist(payment);
			}
		});

		given()
				.when()
				.pathParam("id", carId)
				.get("{id}/overview")
				.then()
				.statusCode(200)
				.body("totalRefuels", is(1))
				// 79.96 refuel + 2 * 40 insurance
				.body("totalCost", is(159.96F));
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
				.statusCode(404);
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
	void shouldGetNoCarsWithoutAccount()
	{
		given().when().get().then().statusCode(200).body("size()", is(0));
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
	void shouldSaveAndClearTheDetails()
	{
		given()
				.body("""
						{ "name": "Normal car", "year": 2019, "odometer": 0, "licensePlate": " M-AB 123 ",
						  "vin": "wvw zzz1kz-6w000001", "purchaseDate": "2020-03-01", "purchasePrice": 18500.5 }
						""")
				.contentType(ContentType.JSON)
				.when()
				.post("{id}", carId)
				.then()
				.statusCode(200)
				.body("licensePlate", is("M-AB 123"))
				.body("vin", is("WVWZZZ1KZ6W000001"))
				.body("purchaseDate", is("2020-03-01"))
				.body("purchasePrice", is(18500.5F));

		given()
				.body("""
						{ "name": "Normal car", "year": 2019, "odometer": 0, "licensePlate": " ", "vin": "" }
						""")
				.contentType(ContentType.JSON)
				.when()
				.post("{id}", carId)
				.then()
				.statusCode(200)
				.body("licensePlate", Matchers.nullValue())
				.body("vin", Matchers.nullValue())
				.body("purchaseDate", Matchers.nullValue())
				.body("purchasePrice", Matchers.nullValue());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldCreateACarWithAShortVinBefore1981()
	{
		given()
				.body("""
						{ "name": "Second car", "year": 1975, "odometer": 0, "vin": "1180012345" }
						""")
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(201);

		assertEquals("1180012345", vinOf("Second car"));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldRejectInvalidDetails()
	{
		String future = LocalDate.now().plusDays(1).toString();
		given()
				.body("""
						{ "name": "Normal car", "year": 2019, "odometer": 0, "licensePlate": "ABCDEFGHIJKLMNOPQRSTU",
						  "vin": "WVWZZZ1KZ6W00000I", "purchaseDate": "%s", "purchasePrice": -1 }
						""".formatted(future))
				.contentType(ContentType.JSON)
				.when()
				.post("{id}", carId)
				.then()
				.statusCode(400)
				.body("size()", is(4));

		// a VIN of 17 characters is required since 1981
		given()
				.body("""
						{ "name": "Second car", "year": 2019, "odometer": 0, "vin": "1180012345" }
						""")
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(400)
				.body("[0]", Matchers.containsString("17"));
	}

	@Transactional
	String vinOf(String name)
	{
		return carRepository.find("name", name).firstResult().getVin();
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
	void shouldFailCreateCarIfAboveMaxCapacity()
	{
		// given: limit was lowered below the amount of existing cars
		QuarkusTransaction.requiringNew().run(() -> accountRepository.findByIdentifier("peter").setMaxCars(0));

		String body = "{\"name\": \"Hansi\", \"year\": \"2000\"}";
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
	void shouldNotUpdateUnknownCar()
	{
		String body = "{\"name\": \"Hans\", \"year\": \"2000\"}";
		given()
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.post("{id}", carId + 1000)
				.then()
				.statusCode(404);
	}

	@Test
	@TestSecurity(user = "bob", roles = {
			"user"
	})
	void shouldNotUpdateCarOfOtherUser()
	{
		String body = "{\"name\": \"Hans\", \"year\": \"2000\"}";
		given()
				.when()
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.post("{id}", carId)
				.then()
				.statusCode(404);
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

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldDeleteMyCarWithItsBills()
	{
		given()
				.when()
				.delete("{id}", carId)
				.then()
				.statusCode(204);

		given()
				.when()
				.get("{id}", carId)
				.then()
				.statusCode(404);

		assertEquals(0, countBills());
	}

	@Test
	@TestSecurity(user = "bob", roles = {
			"user"
	})
	void shouldNotDeleteCarOfOtherUser()
	{
		given()
				.when()
				.delete("{id}", carId)
				.then()
				.statusCode(404);

		assertEquals(1, countBills());
	}

	@Transactional
	long countBills()
	{
		return billRepository.count("car.id", carId);
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}
}
