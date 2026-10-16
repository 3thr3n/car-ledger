package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

/**
 * Like in production, an access token has to name CarLedger as audience: a
 * token of another client of the realm is rejected. The tokens of the Keycloak
 * dev service have no audience.
 */
@QuarkusTest
@TestProfile(BearerAudienceTest.WithAudience.class)
class BearerAudienceTest
{
	public static class WithAudience implements QuarkusTestProfile
	{
		@Override
		public Map<String, String> getConfigOverrides()
		{
			return Map.of("quarkus.oidc.token.audience", "carLedger");
		}
	}

	private final KeycloakTestClient keycloak = new KeycloakTestClient();

	@Test
	void shouldRejectATokenForAnotherClient()
	{
		given()
				.auth().oauth2(keycloak.getAccessToken("alice"))
				.redirects().follow(false)
				.when()
				.get("/api/admin/stats")
				.then()
				.statusCode(401);
	}
}
