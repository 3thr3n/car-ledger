package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class VersionResourceTest
{
	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldTellTheVersionOfTheBuild()
	{
		// the revision of the pom, "dev" without one
		given()
				.when()
				.get("/api/version")
				.then()
				.statusCode(200)
				.body("version", Matchers.is("dev"));
	}
}
