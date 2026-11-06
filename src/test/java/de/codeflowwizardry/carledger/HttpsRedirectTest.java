package de.codeflowwizardry.carledger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.http.HttpMethod;

@QuarkusTest
class HttpsRedirectTest
{
	@Test
	void shouldOnlyRedirectReadsWithHttpThroughTheGateway()
	{
		assertTrue(HttpsRedirect.isPlainHttp(HttpMethod.GET, "http", "car-ledger.example.com"));
		assertTrue(HttpsRedirect.isPlainHttp(HttpMethod.HEAD, "HTTP", "car-ledger.example.com"));

		assertFalse(HttpsRedirect.isPlainHttp(HttpMethod.GET, "https", "car-ledger.example.com"));
		// the probes, dev and the tests: no gateway
		assertFalse(HttpsRedirect.isPlainHttp(HttpMethod.GET, null, "localhost:8080"));
		// a change would lose its body
		assertFalse(HttpsRedirect.isPlainHttp(HttpMethod.POST, "http", "car-ledger.example.com"));
		assertFalse(HttpsRedirect.isPlainHttp(HttpMethod.GET, "http", null));
	}

	@Test
	void shouldRedirectToTheSamePathWithHttps()
	{
		given().redirects().follow(false).header("X-Forwarded-Proto", "http")
				.header("Host", "car-ledger.example.com")
				.get("/car/1/trips?page=2")
				.then().statusCode(301).header("Location", is("https://car-ledger.example.com/car/1/trips?page=2"));
	}

	@Test
	void shouldServeWithoutTheGatewayAndWithHttps()
	{
		given().redirects().follow(false).get("/q/health/live").then().statusCode(not(301));
		given().redirects().follow(false).header("X-Forwarded-Proto", "https").get("/q/health/live").then()
				.statusCode(not(301));
	}
}
