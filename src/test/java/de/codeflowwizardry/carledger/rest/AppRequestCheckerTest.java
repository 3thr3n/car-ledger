package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AppRequestCheckerTest
{
	@Test
	void shouldOnlyTreatTheFrontendAsJavaScript()
	{
		assertTrue(AppRequestChecker.isApp("JavaScript", null));
		assertTrue(AppRequestChecker.isApp("JavaScript", "cors"));
		assertTrue(AppRequestChecker.isApp("JavaScript", "same-origin"));

		// a browser built into an app, e.g. of WhatsApp
		assertFalse(AppRequestChecker.isApp("XMLHttpRequest", null));
		assertFalse(AppRequestChecker.isApp("com.whatsapp", null));
		assertFalse(AppRequestChecker.isApp(null, null));
		// a page is opened, never a request of the frontend
		assertFalse(AppRequestChecker.isApp("JavaScript", "navigate"));
	}

	@Test
	void shouldRedirectTheLoginOfABuiltInBrowser()
	{
		given().redirects().follow(false).header("X-Requested-With", "XMLHttpRequest")
				.get("/api/auth/login")
				.then().statusCode(302).header("Location", containsString("/auth"));

		given().redirects().follow(false).header("X-Requested-With", "JavaScript")
				.header("Sec-Fetch-Mode", "navigate")
				.get("/api/auth/login")
				.then().statusCode(302);
	}

	@Test
	void shouldAnswerTheFrontendWith499()
	{
		given().redirects().follow(false).header("X-Requested-With", "JavaScript")
				.get("/api/auth/login")
				.then().statusCode(499);
	}
}
