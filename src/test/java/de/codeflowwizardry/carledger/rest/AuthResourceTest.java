package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;

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
}
