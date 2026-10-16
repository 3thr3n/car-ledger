package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

/**
 * Machines (a script with a service account) call the API with an access token
 * instead of the session of the browser. Tokens of the Keycloak dev service:
 * alice has the role admin, bob only user. Redirects are not followed: without
 * a valid token the login page of Keycloak would answer with 200.
 */
@QuarkusTest
class BearerTokenTest
{
	private final KeycloakTestClient keycloak = new KeycloakTestClient();

	@Test
	void shouldOpenTheAdminApiWithTheTokenOfAnAdmin()
	{
		given()
				.auth().oauth2(keycloak.getAccessToken("alice"))
				.redirects().follow(false)
				.when()
				.get("/api/admin/stats")
				.then()
				.statusCode(200)
				.body("accounts", notNullValue());
	}

	@Test
	void shouldNotOpenTheAdminApiWithTheTokenOfAUser()
	{
		given()
				.auth().oauth2(keycloak.getAccessToken("bob"))
				.redirects().follow(false)
				.when()
				.get("/api/admin/stats")
				.then()
				.statusCode(403);
	}

	@Test
	void shouldRejectAnInvalidToken()
	{
		given()
				.auth().oauth2("not-a-token")
				.redirects().follow(false)
				.when()
				.get("/api/admin/stats")
				.then()
				.statusCode(401);
	}

	@Test
	void shouldKeepTheLoginOfTheBrowserWithoutAToken()
	{
		// requests of the frontend without a session get 499 instead of a redirect
		given()
				.header("X-Requested-With", "JavaScript")
				.redirects().follow(false)
				.when()
				.get("/api/admin/stats")
				.then()
				.statusCode(499);
	}
}
