package de.codeflowwizardry.carledger.rest.car.maintenance;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.TestDataProfile;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(TestDataProfile.class)
@TestHTTPEndpoint(MaintenanceResource.class)
class MaintenanceResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Test
	@TestSecurity(user = "alice", roles = {
			"user"
	})
	void shouldGetAllMyBillsInOrder()
	{
		Long id = accountRepository.findByIdentifier("alice").getCarList().get(0).getId();

		given()
				.pathParam("carId", id)
				.when()
				.get("all")
				.then()
				.statusCode(200)
				.body("total", is(0))
				.body("page", is(1))
				.body("size", is(10))
				.body("data.size()", is(0));
	}

	@Test
	@TestSecurity(user = "alice", roles = {
			"user"
	})
	void shouldGetYearsOfBills()
	{
		Long id = accountRepository.findByIdentifier("alice").getCarList().get(0).getId();

		// when
		given()
				.pathParam("carId", id)
				.when()
				.get("years")
				.then()
				.statusCode(200)
				.body("size()", is(0));
	}
}
