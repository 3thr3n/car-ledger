package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.http.HttpMethod;

@QuarkusTest
class RequestedWithTest
{
	@Test
	void shouldRejectAChangeOfTheSessionWithoutTheHeader()
	{
		assertTrue(RequestedWith.isForged(HttpMethod.POST, "q_session=abc", null));
		assertTrue(RequestedWith.isForged(HttpMethod.DELETE, "a=b; q_session_chunk_1=abc", ""));
		assertTrue(RequestedWith.isForged(HttpMethod.PUT, "q_session=abc", " "));
	}

	@Test
	void shouldAcceptTheAppTokensAndReads()
	{
		assertFalse(RequestedWith.isForged(HttpMethod.POST, "q_session=abc", "JavaScript"));
		// a service account or a tracker, no cookie of the browser
		assertFalse(RequestedWith.isForged(HttpMethod.POST, null, null));
		assertFalse(RequestedWith.isForged(HttpMethod.GET, "q_session=abc", null));
		assertFalse(RequestedWith.isForged(HttpMethod.HEAD, "q_session=abc", null));
	}

	@Test
	void shouldAnswer403BeforeTheResource()
	{
		// a form of another host of the site, e.g. an upload as multipart
		given().cookie("q_session", "abc")
				.multiPart("file", "receipt.pdf", new byte[] {
						1, 2, 3
				})
				.post("/api/attachment/1/bill/1")
				.then().statusCode(403).body(containsString("X-Requested-With"));

		given().cookie("q_session", "abc").header("X-Requested-With", "JavaScript")
				.multiPart("file", "receipt.pdf", new byte[] {
						1, 2, 3
				})
				.post("/api/attachment/1/bill/1")
				// passed on to the login, which doesn't know the session
				.then().statusCode(not(403));
	}
}
