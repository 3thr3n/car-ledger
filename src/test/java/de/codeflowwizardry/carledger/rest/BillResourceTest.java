package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.Bill;
import de.codeflowwizardry.carledger.data.Car;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class BillResourceTest extends AbstractResourceTest
{
	@Inject
	BillRepository billRepository;

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	Car car;

	@BeforeEach
	@Transactional
	void setup()
	{
		cleanup();

		setupBob();
	}

	private void setupBob()
	{
		Account account = new Account();
		account.setMaxCars(1);
		account.setUserId("bob");
		accountRepository.persist(account);

		car = new Car();
		car.setUser(account);
		car.setDescription("Neat car");
		carRepository.persist(car);

		Bill bill = new Bill();
		bill.setEstimate(BigDecimal.valueOf(8.5));
		bill.setDay(LocalDate.of(2024, 8, 16));
		bill.setDistance(BigDecimal.valueOf(500));
		bill.setUnit(BigDecimal.valueOf(28d));
		bill.setPricePerUnit(BigDecimal.valueOf(199.9d));
		bill.setCar(car);
		billRepository.persist(bill);

		bill = new Bill();
		bill.setEstimate(BigDecimal.valueOf(9.1d));
		bill.setDay(LocalDate.of(2022, 5, 22));
		bill.setDistance(BigDecimal.valueOf(400));
		bill.setUnit(BigDecimal.valueOf(20d));
		bill.setPricePerUnit(BigDecimal.valueOf(189.9d));
		bill.setCar(car);
		billRepository.persist(bill);

		bill = new Bill();
		bill.setEstimate(BigDecimal.valueOf(8.2d));
		bill.setDay(LocalDate.of(2023, 6, 2));
		bill.setDistance(BigDecimal.valueOf(480));
		bill.setUnit(BigDecimal.valueOf(28d));
		bill.setPricePerUnit(BigDecimal.valueOf(196.9d));
		bill.setCar(car);
		billRepository.persist(bill);
	}

	@Test
	void shouldGetAllMyBillsInOrder()
	{
		given()
				.cookie(bobCookie)
				.when()
				.get("/api/bill/" + car.getId() + "/all")
				.then()
				.statusCode(200)
				.body("total", is(3))
				.body("page", is(1))
				.body("size", is(10))
				.body("data.size()", is(3))
				.body("data[0].distance", is("500.00"))
				.body("data[0].day", is("2024-08-16"))
				.body("data[1].distance", is("480.00"))
				.body("data[1].day", is("2023-06-02"))
				.body("data[2].distance", is("400.00"))
				.body("data[2].day", is("2022-05-22"));
	}

	@Test
	void shouldAddBill()
	{
		// given
		assertEquals(3, billRepository.count());

		String body = """
				{
					"day": "2024-08-22",
					"distance": 450,
					"unit": 30,
					"pricePerUnit": 188.9,
					"estimate": 8.9
				}
				""";

		// when
		given()
				.cookie(bobCookie)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(body)
				.put("/api/bill/" + car.getId())
				.then()
				.statusCode(202)
				.body("day", is("2024-08-22"));

		// then
		assertEquals(4, billRepository.count());
	}

	@Test
	void shouldFailAddingBillAsDifferentUser()
	{
		// given
		assertEquals(3, billRepository.count());

		String body = """
				{
					"day": "2024-08-22",
					"distance": 450,
					"unit": 30,
					"pricePerUnit": 188.9,
					"estimate": 8.9
				}
				""";

		// when
		given()
				.cookie(aliceCookie)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(body)
				.put("/api/bill/" + car.getId())
				.then()
				.statusCode(400);

		// then
		assertEquals(3, billRepository.count());
	}

	@Test
	void shouldDeleteOne()
	{
		// given
		assertEquals(3, billRepository.count());

		Long billId = billRepository.listAll().getFirst().getId();

		// when
		given()
				.cookie(bobCookie)
				.when()
				.delete("/api/bill/" + car.getId() + "/" + billId)
				.then()
				.statusCode(202);

		// then
		assertEquals(2, billRepository.count());
	}

	@Test
	void shouldFailDeletingAsDifferentUser()
	{
		// given
		assertEquals(3, billRepository.count());

		Long billId = billRepository.listAll().getFirst().getId();

		// when
		given()
				.cookie(aliceCookie)
				.when()
				.delete("/api/bill/" + car.getId() + "/" + billId)
				.then()
				.statusCode(400);

		// then
		assertEquals(3, billRepository.count());
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		billRepository.deleteAll();
		carRepository.deleteAll();
		accountRepository.deleteAll();
	}
}
