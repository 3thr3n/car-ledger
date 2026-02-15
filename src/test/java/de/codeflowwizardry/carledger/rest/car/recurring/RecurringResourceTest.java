package de.codeflowwizardry.carledger.rest.car.recurring;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillRepository;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestHTTPEndpoint(RecurringResource.class)
class RecurringResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	RecurringBillRepository recurringBillRepository;

	Long carId;

	@BeforeEach
	@Transactional
	void setup()
	{
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setMaxCars(2);
		accountEntity.setUserId("peter");
		accountRepository.persist(accountEntity);

		CarEntity carEntity = new CarEntity();
		carEntity.setUser(accountEntity);
		carEntity.setName("Normal car");
		carRepository.persist(carEntity);

		carId = carEntity.getId();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		recurringBillRepository.deleteAll();
		carRepository.deleteAll();
		accountRepository.deleteAll();
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldGetAllMyRecurringBills()
	{
		given()
				.pathParam("carId", carId)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("size()", is(0));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldAddNewRecurringBill()
	{
		String body = """
				{
				     "name": "Finance payments",
					 "billInterval": "MONTHLY",
					 "category": "FINANCE",
					 "startDate": "2025-01-01",
					 "endDate": "2025-12-31",
					 "amount": 500.00
				 }
				""";

		given()
				.pathParam("carId", carId)
				.body(body)
				.accept(ContentType.JSON)
				.contentType(ContentType.JSON)
				.when()
				.put()
				.then()
				.statusCode(202)
				.body("total", is(6000.0F));
	}
}
