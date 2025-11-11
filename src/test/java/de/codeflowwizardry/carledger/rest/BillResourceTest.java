package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import de.codeflowwizardry.carledger.data.BillEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class BillResourceTest
{
	@Inject
	BillRepository billRepository;

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

		setupBob();
		setupPeter();
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

		BillEntity billEntity = new BillEntity();
		billEntity.setEstimate(BigDecimal.valueOf(8.5));
		billEntity.setDay(LocalDate.of(2024, 8, 16));
		billEntity.setDistance(BigDecimal.valueOf(500));
		billEntity.setUnit(BigDecimal.valueOf(28d));
		billEntity.setPricePerUnit(BigDecimal.valueOf(199.9d));
		billEntity.setCar(carEntity);
		billRepository.persist(billEntity);

		billEntity = new BillEntity();
		billEntity.setEstimate(BigDecimal.valueOf(9.1d));
		billEntity.setDay(LocalDate.of(2022, 5, 22));
		billEntity.setDistance(BigDecimal.valueOf(400));
		billEntity.setUnit(BigDecimal.valueOf(20d));
		billEntity.setPricePerUnit(BigDecimal.valueOf(189.9d));
		billEntity.setCar(carEntity);
		billRepository.persist(billEntity);

		billEntity = new BillEntity();
		billEntity.setEstimate(BigDecimal.valueOf(8.2d));
		billEntity.setDay(LocalDate.of(2023, 6, 2));
		billEntity.setDistance(BigDecimal.valueOf(480));
		billEntity.setUnit(BigDecimal.valueOf(28d));
		billEntity.setPricePerUnit(BigDecimal.valueOf(196.9d));
		billEntity.setCar(carEntity);
		billRepository.persist(billEntity);
	}

	private void setupBob()
	{
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setMaxCars(1);
		accountEntity.setUserId("bob");
		accountRepository.persist(accountEntity);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllMyBillsInOrder()
	{
		given()
				.when()
				.get("/api/bill/" + carEntity.getId() + "/all")
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
	@TestSecurity(user = "peter", roles = {
			"user"
	})
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
		given().accept(ContentType.JSON).contentType(ContentType.JSON).body(body).put("/api/bill/" + carEntity.getId()).then()
				.statusCode(202).body("day", is("2024-08-22"));

		// then
		assertEquals(4, billRepository.count());
	}

	@Test
	@TestSecurity(user = "bob", roles = {
			"user"
	})
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
		given().accept(ContentType.JSON).contentType(ContentType.JSON).body(body).put("/api/bill/" + carEntity.getId()).then()
				.statusCode(400);

		// then
		assertEquals(3, billRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldDeleteOne()
	{
		// given
		assertEquals(3, billRepository.count());

		Long billId = billRepository.listAll().getFirst().getId();

		// when
		given().delete("/api/bill/" + carEntity.getId() + "/" + billId).then().statusCode(202);

		// then
		assertEquals(2, billRepository.count());
	}

	@Test
	@TestSecurity(user = "bob", roles = {
			"user"
	})
	void shouldFailDeletingAsDifferentUser()
	{
		// given
		assertEquals(3, billRepository.count());

		Long billId = billRepository.listAll().getFirst().getId();

		// when
		given().delete("/api/bill/" + carEntity.getId() + "/" + billId).then().statusCode(400);

		// then
		assertEquals(3, billRepository.count());
	}

	@Test
	void shouldBeRedirectToLoginUrl()
	{
		String response = given().when().get("/api/bill/" + carEntity.getId() + "/all").then().statusCode(200).extract()
				.response().getBody().asString();
		assertTrue(response.contains("<html"));
	}

    @Test
    @TestSecurity(user = "peter", roles = {
            "user"
    })
    void shouldGetYearsOfBills()
    {
        // given
        assertEquals(3, billRepository.count());

        // when
        given()
                .get("/api/bill/" + carEntity.getId() + "/years")
                .then()
                .statusCode(200)
                .body("", contains(2024, 2023, 2022));

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
