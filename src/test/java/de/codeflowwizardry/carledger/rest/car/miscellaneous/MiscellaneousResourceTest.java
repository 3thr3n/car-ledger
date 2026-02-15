package de.codeflowwizardry.carledger.rest.car.miscellaneous;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.math.BigInteger;
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
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestHTTPEndpoint(MiscellaneousResource.class)
class MiscellaneousResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	BillRepository billRepository;

	@Inject
	MiscellaneousBillFactory factory;

	Long carId;

	@BeforeEach
	@Transactional
	void setup()
	{
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setMaxCars(1);
		accountEntity.setUserId("alice");
		accountRepository.persist(accountEntity);

		CarEntity carEntity = new CarEntity();
		carEntity.setUser(accountEntity);
		carEntity.setName("Neat car");
		carRepository.persist(carEntity);

		MiscellaneousBillInput input = new MiscellaneousBillInput(BigDecimal.valueOf(50), BigInteger.ZERO,
				LocalDate.now(), "");

		carId = carEntity.getId();
		factory.create(input, carId, "alice");
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		billRepository.deleteAll();
		carRepository.deleteAll();
		accountRepository.deleteAll();
	}

	@Test
	@TestSecurity(user = "alice", roles = {
			"user"
	})
	void shouldGetAllMyBillsInOrder()
	{
		given()
				.pathParam("carId", carId)
				.when()
				.get("all")
				.then()
				.statusCode(200)
				.body("total", is(1))
				.body("page", is(1))
				.body("size", is(10))
				.body("data.size()", is(1));
	}

	@Test
	@TestSecurity(user = "alice", roles = {
			"user"
	})
	void shouldGetYearsOfBills()
	{
		// when
		given()
				.pathParam("carId", carId)
				.when()
				.get("years")
				.then()
				.statusCode(200)
				.body("size()", is(1))
				.body("", contains(LocalDate.now().getYear()));
	}

	@Test
	@TestSecurity(user = "alice", roles = {
			"user"
	})
	void shouldAddBill()
	{
		// given
		String body = """
				{
					"date": "2024-08-22",
					"vatRate": 19,
					"total": 200,
					"description": "Something, Something"
				}
				""";

		// when
		given()
				.pathParam("carId", carId)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(body)
				.put()
				.then()
				.statusCode(202)
				.body("date", is("2024-08-22"));

	}

	@Test
	@TestSecurity(user = "bob", roles = {
			"user"
	})
	void shouldFailAddingBillAsDifferentUser()
	{
		// given
		String body = """
				{
					"date": "2024-08-22",
					"vatRate": 19,
					"total": 200,
					"odometer": 25999,
					"workshop": "Hans Peters Workshop"
				}
				""";

		// when
		given()
				.pathParam("carId", carId)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.body(body)
				.put()
				.then()
				.statusCode(400);
	}
}
