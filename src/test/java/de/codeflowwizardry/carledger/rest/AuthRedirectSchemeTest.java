package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AuthRedirectSchemeTest
{
	@Test
	void shouldRedirectBackFromTheLoginWithTheSchemeOfTheGateway()
	{
		// TLS ends at the gateway, the app itself is called with http
		given().redirects().follow(false).header("X-Forwarded-Proto", "https")
				.get("/api/auth/callback")
				.then().statusCode(302).header("Location", startsWith("https://"));
	}

	@Test
	void shouldKeepTheSchemeWithoutAGateway()
	{
		given().redirects().follow(false)
				.get("/api/auth/callback")
				.then().statusCode(302).header("Location", startsWith("http://"));
	}
}
