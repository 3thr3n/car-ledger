package de.codeflowwizardry.carledger.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
public class AuthResourceTest
{
	@Test
	void shouldlogin()
	{
		given().when().get("/api/auth/login").then().statusCode(200);
	}

	@Test
	void shouldCallbackRedirect()
	{
		given().redirects().follow(false).when().get("/api/auth/callback").then().statusCode(302);
	}
}
