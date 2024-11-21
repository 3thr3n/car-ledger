package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.TestDataProfile;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
@TestProfile(TestDataProfile.class)
@TestHTTPEndpoint(BillResource.class)
class BillResourcePaginationTest
{
	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldGetFirstPage()
	{
		given()
				.when()
				.pathParam("carId", 5L)
				.queryParam("page", 1)
				.queryParam("size", 5)
				.get("all")
				.then()
				.statusCode(200)
				.body("total", is(71))
				.body("data.size()", is(5));
	}

	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldGetThirdPage()
	{
		given()
				.when()
				.pathParam("carId", 5L)
				.queryParam("page", 5)
				.queryParam("size", 15)
				.get("all")
				.then()
				.statusCode(200)
				.body("data.size()", is(11));
	}

	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldNotAllowInvalidPage()
	{
		given()
				.when()
				.pathParam("carId", 5L)
				.queryParam("page", 0)
				.queryParam("size", 8)
				.get("all")
				.then()
				.statusCode(200)
				.body("data.size()", is(8));

		given()
				.when()
				.pathParam("carId", 5L)
				.queryParam("page", 10)
				.queryParam("size", 8)
				.get("all")
				.then()
				.statusCode(200)
				.body("data.size()", is(0));
	}
}
