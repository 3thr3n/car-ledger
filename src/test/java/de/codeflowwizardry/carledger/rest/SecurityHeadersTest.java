package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class SecurityHeadersTest
{
	private static final String CORP = "Cross-Origin-Resource-Policy";

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldKeepTheAnswersOfTheApiToTheOwnOrigin()
	{
		given().when().get("/api/version").then()
				.statusCode(200)
				.header(CORP, is("same-origin"))
				.header("X-Content-Type-Options", is("nosniff"));
	}

	@Test
	void shouldKeepTheErrorsOfTheApiToTheOwnOrigin()
	{
		given().header("X-Requested-With", "JavaScript").when().get("/api/car/my").then()
				.statusCode(499)
				.header(CORP, is("same-origin"));
	}

	@Test
	void shouldNotRestrictThePagesOfTheApp()
	{
		// a page is opened, not loaded by another one; the CSP restricts its frames
		given().when().get("/").then()
				.header(CORP, nullValue());
	}
}
