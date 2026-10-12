package de.codeflowwizardry.carledger.rest.admin;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.MiscellaneousBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.miscellaneous.MiscellaneousBillInput;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestHTTPEndpoint(AdminResource.class)
class AdminResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	BillRepository billRepository;

	@Inject
	MiscellaneousBillFactory miscellaneousBillFactory;

	long adminId;
	long peterId;

	@BeforeEach
	@Transactional
	void setup()
	{
		AccountEntity admin = new AccountEntity();
		admin.setUserId("alice");
		admin.setMaxCars(1);
		accountRepository.persist(admin);
		adminId = admin.getId();

		AccountEntity peter = new AccountEntity();
		peter.setUserId("peter");
		peter.setMaxCars(1);
		accountRepository.persist(peter);
		peterId = peter.getId();

		CarEntity car = new CarEntity();
		car.setUser(peter);
		car.setName("Peters car");
		carRepository.persist(car);

		miscellaneousBillFactory.create(
				new MiscellaneousBillInput(BigDecimal.TEN, 19, LocalDate.of(2025, 1, 1), "Parking"),
				car.getId(), "peter");
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	@Test
	@TestSecurity(user = "alice", roles = {
			"user", "admin"
	})
	void shouldGetStats()
	{
		given()
				.when()
				.get("stats")
				.then()
				.statusCode(200)
				.body("accounts", is(2))
				.body("cars", is(1))
				.body("bills", is(1))
				.body("recurringBills", is(0));
	}

	@Test
	@TestSecurity(user = "alice", roles = {
			"user", "admin"
	})
	void shouldListAccountsWithUsage()
	{
		given()
				.when()
				.get("accounts")
				.then()
				.statusCode(200)
				.body("total", is(2))
				.body("data[0].name", is("alice"))
				.body("data[1].name", is("peter"))
				.body("data[1].cars", is(1))
				.body("data[1].bills", is(1));

		given()
				.queryParam("search", "PET")
				.when()
				.get("accounts")
				.then()
				.statusCode(200)
				.body("total", is(1))
				.body("data[0].name", is("peter"));
	}

	@Test
	@TestSecurity(user = "alice", roles = {
			"user", "admin"
	})
	void shouldChangeMaxCars()
	{
		given()
				.contentType(ContentType.JSON)
				.body("{ \"maxCars\": 3 }")
				.when()
				.post("accounts/" + peterId)
				.then()
				.statusCode(200)
				.body("maxCars", is(3))
				.body("cars", is(1));

		given()
				.contentType(ContentType.JSON)
				.body("{ \"maxCars\": 101 }")
				.when()
				.post("accounts/" + peterId)
				.then()
				.statusCode(400);
	}

	@Test
	@TestSecurity(user = "alice", roles = {
			"user", "admin"
	})
	void shouldDeleteAccountWithItsData()
	{
		given()
				.when()
				.delete("accounts/" + peterId)
				.then()
				.statusCode(204);

		assertEquals(1, accountRepository.count());
		assertEquals(0, countBills());
	}

	@Test
	@TestSecurity(user = "alice", roles = {
			"user", "admin"
	})
	void shouldNotDeleteOwnAccount()
	{
		given()
				.when()
				.delete("accounts/" + adminId)
				.then()
				.statusCode(400);

		given()
				.when()
				.delete("accounts/999999")
				.then()
				.statusCode(404);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldForbidUsers()
	{
		given().when().get("stats").then().statusCode(403);
		given().when().get("accounts").then().statusCode(403);
		given().when().delete("accounts/" + adminId).then().statusCode(403);
	}

	@Transactional
	long countBills()
	{
		return billRepository.count();
	}
}
