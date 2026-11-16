package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class LegalResourceTest
{
	@Test
	void shouldLinkTheLegalPagesWithoutALogin()
	{
		// the landing page shows them before the login
		given().get("/api/legal")
				.then().statusCode(200)
				.body("imprintUrl", is("https://example.com/impressum"))
				// not set up in the tests
				.body("privacyUrl", nullValue());
	}

	@Test
	void shouldKeepTheRestOfTheApiBehindTheLogin()
	{
		given().redirects().follow(false).get("/api/version").then().statusCode(302);
		given().redirects().follow(false).get("/api/legalese").then().statusCode(302);
	}
}
