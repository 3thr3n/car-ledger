package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
@TestHTTPEndpoint(AuthResource.class)
@TestSecurity(authorizationEnabled = false)
class AuthResourceTest
{
	@Test
	void shouldLogin()
	{
		given()
				.redirects()
				.follow(false)
				.when()
				.get("login")
				.then()
				.statusCode(302);
	}

	@Test
	void shouldCallbackRedirect()
	{
		given()
				.redirects()
				.follow(false)
				.when()
				.get("callback")
				.then()
				.statusCode(302);
	}

	@Test
	void shouldShowTheErrorOfTheProvider()
	{
		given()
				.redirects()
				.follow(false)
				.queryParam("error", "invalid_request")
				.queryParam("error_description", "The request is otherwise malformed")
				.when()
				.get("error")
				.then()
				.statusCode(302)
				.header("Location", endsWith("/error?reason=The+request+is+otherwise+malformed"));
	}

	@Test
	void shouldShowTheErrorCodeWithoutDescription()
	{
		given()
				.redirects()
				.follow(false)
				.queryParam("error", "access_denied")
				.when()
				.get("error")
				.then()
				.statusCode(302)
				.header("Location", endsWith("/error?reason=access_denied"));
	}
}
