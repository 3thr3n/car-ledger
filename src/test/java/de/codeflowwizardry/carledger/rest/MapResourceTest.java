package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
@TestSecurity(user = "peter", roles = {
		"user"
})
class MapResourceTest
{
	@Test
	void shouldHideTheMapWithoutAStyle()
	{
		given().when().get("/api/map").then()
				.statusCode(200)
				.body("styleUrl", nullValue());
	}
}
