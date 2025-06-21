package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.TestDataProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(TestDataProfile.class)
class BillResourcePaginationTest  extends AbstractResourceTest
{
	@Test
	void shouldGetFirstPage()
	{
		given()
				.cookie(aliceCookie)
				.when()
				.pathParam("carId", 5L)
				.queryParam("page", 1)
				.queryParam("size", 5)
				.get("api/bill/{carId}/all")
				.then()
				.statusCode(200)
				.body("total", is(71))
				.body("data.size()", is(5));
	}

	@Test
	void shouldGetThirdPage()
	{
		given()
				.cookie(aliceCookie)
				.when()
				.pathParam("carId", 5L)
				.queryParam("page", 5)
				.queryParam("size", 15)
				.get("api/bill/{carId}/all")
				.then()
				.statusCode(200)
				.body("data.size()", is(11));
	}

	@Test
	void shouldNotAllowInvalidPage()
	{
		given()
				.cookie(aliceCookie)
				.when()
				.pathParam("carId", 5L)
				.queryParam("page", 0)
				.queryParam("size", 8)
				.get("api/bill/{carId}/all")
				.then()
				.statusCode(200)
				.body("data.size()", is(8));

		given()
				.cookie(aliceCookie)
				.when()
				.pathParam("carId", 5L)
				.queryParam("page", 10)
				.queryParam("size", 8)
				.get("api/bill/{carId}/all")
				.then()
				.statusCode(200)
				.body("data.size()", is(0));
	}
}
