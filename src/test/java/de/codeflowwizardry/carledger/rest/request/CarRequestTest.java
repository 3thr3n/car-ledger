package de.codeflowwizardry.carledger.rest.request;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.request.CarLimitRequests;
import de.codeflowwizardry.carledger.request.RequestService;
import de.codeflowwizardry.carledger.request.RequestType;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Peter has reached his limit of one car and requests more, alice decides as
 * administrator
 */
@QuarkusTest
class CarRequestTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	CarLimitRequests carLimitRequests;

	@Inject
	RequestService requestService;

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();
		account("alice", 1);
		AccountEntity peter = account("peter", 1);
		account("bob", 1);

		CarEntity car = new CarEntity();
		car.setUser(peter);
		car.setName("Peters car");
		carRepository.persist(car);
	}

	private AccountEntity account(String name, int maxCars)
	{
		AccountEntity account = new AccountEntity();
		account.setUserId(name);
		account.setMaxCars(maxCars);
		accountRepository.persist(account);
		return account;
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	/** peters request, created as he would */
	@Transactional
	long peterRequests(int cars)
	{
		return carLimitRequests.request(accountRepository.findByIdentifier("peter"), cars, "Second car for my wife")
				.getId();
	}

	@Transactional
	int maxCarsOf(String name)
	{
		return accountRepository.findByIdentifier(name).getMaxCars();
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldRequestMoreCarsOnlyOnce()
	{
		given().when().get("/api/user/me/car-requests/latest").then().statusCode(204);

		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "cars": 2, "reason": " Second car for my wife " }
						""")
				.when()
				.post("/api/user/me/car-requests")
				.then()
				.statusCode(201)
				.body("cars", is(2))
				.body("maxCars", is(1))
				.body("reason", is("Second car for my wife"))
				.body("state", is("OPEN"));

		given()
				.when()
				.get("/api/user/me/car-requests/latest")
				.then()
				.statusCode(200)
				.body("state", is("OPEN"))
				.body("grantedMaxCars", nullValue());

		// only one open request
		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "cars": 1 }
						""")
				.when()
				.post("/api/user/me/car-requests")
				.then()
				.statusCode(409);
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldRejectInvalidRequests()
	{
		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "cars": 0 }
						""")
				.when()
				.post("/api/user/me/car-requests")
				.then()
				.statusCode(400)
				.body("[0]", is("Request between 1 and 99 cars!"));
	}

	@Test
	@TestSecurity(user = "bob", roles = "user")
	void shouldOnlyRequestWithAtMostOneFreeCar()
	{
		// bob has no car yet, one is free: close to the limit
		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "cars": 1 }
						""")
				.when()
				.post("/api/user/me/car-requests")
				.then()
				.statusCode(201);
	}

	@Test
	@TestSecurity(user = "alice", roles = "admin")
	void shouldApproveAndRaiseTheLimit()
	{
		long id = peterRequests(2);

		given()
				.when()
				.get("/api/admin/stats")
				.then()
				.statusCode(200)
				.body("openRequests", is(1));
		given()
				.queryParam("state", "OPEN")
				.when()
				.get("/api/admin/car-requests")
				.then()
				.statusCode(200)
				.body("total", is(1))
				.body("data.account", contains("peter"))
				.body("data[0].currentCars", is(1))
				.body("data[0].currentMaxCars", is(1))
				.body("data[0].reason", is("Second car for my wife"));

		// without a limit: the current one plus the requested cars
		given()
				.contentType(ContentType.JSON)
				.body("{}")
				.when()
				.post("/api/admin/car-requests/{id}/approve", id)
				.then()
				.statusCode(200)
				.body("state", is("APPROVED"))
				.body("grantedMaxCars", is(3))
				.body("currentMaxCars", is(3))
				.body("decidedBy", is("alice"));
		assertEquals(3, maxCarsOf("peter"));

		// decided once
		given()
				.contentType(ContentType.JSON)
				.body("{}")
				.when()
				.post("/api/admin/car-requests/{id}/reject", id)
				.then()
				.statusCode(409);
		given().when().get("/api/admin/stats").then().body("openRequests", is(0));
	}

	@Test
	@TestSecurity(user = "alice", roles = "admin")
	void shouldApproveWithAnOwnLimit()
	{
		long id = peterRequests(5);

		// not above the current limit
		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "maxCars": 1 }
						""")
				.when()
				.post("/api/admin/car-requests/{id}/approve", id)
				.then()
				.statusCode(400);

		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "maxCars": 2 }
						""")
				.when()
				.post("/api/admin/car-requests/{id}/approve", id)
				.then()
				.statusCode(200)
				.body("cars", is(5))
				.body("grantedMaxCars", is(2));
		assertEquals(2, maxCarsOf("peter"));
	}

	@Test
	@TestSecurity(user = "alice", roles = "admin")
	void shouldRejectWithAComment()
	{
		long id = peterRequests(1);

		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "comment": "One car is enough" }
						""")
				.when()
				.post("/api/admin/car-requests/{id}/reject", id)
				.then()
				.statusCode(200)
				.body("state", is("REJECTED"))
				.body("comment", is("One car is enough"));
		assertEquals(1, maxCarsOf("peter"));

		given()
				.queryParam("state", "REJECTED")
				.when()
				.get("/api/admin/car-requests")
				.then()
				.body("total", is(1));
		given()
				.contentType(ContentType.JSON)
				.body("{}")
				.when()
				.post("/api/admin/car-requests/{id}/approve", 999_999)
				.then()
				.statusCode(404);
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldNotBeAllowedToDecide()
	{
		long id = peterRequests(1);

		given()
				.contentType(ContentType.JSON)
				.body("{}")
				.when()
				.post("/api/admin/car-requests/{id}/approve", id)
				.then()
				.statusCode(403);
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldShowTheDecisionOnce()
	{
		long id = peterRequests(1);
		rejectAsAdmin(id);

		given()
				.when()
				.get("/api/user/me/car-requests/latest")
				.then()
				.statusCode(200)
				.body("state", is("REJECTED"))
				.body("comment", is("Not now"))
				.body("outcomeSeen", is(false));

		given().when().post("/api/user/me/car-requests/{id}/seen", id).then().statusCode(204);
		given()
				.when()
				.get("/api/user/me/car-requests/latest")
				.then()
				.body("outcomeSeen", is(true));
	}

	@Test
	@TestSecurity(user = "bob", roles = "user")
	void shouldNotSeeTheRequestsOfOthers()
	{
		long id = peterRequests(1);

		given().when().post("/api/user/me/car-requests/{id}/seen", id).then().statusCode(404);
	}

	void rejectAsAdmin(long id)
	{
		requestService.reject(id, RequestType.CAR_LIMIT, "Not now", "alice");
	}
}
