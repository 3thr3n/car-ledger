package de.codeflowwizardry.carledger.webhook;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.request.CarLimitRequests;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * The car requests send their events to a receiver, after the commit
 */
@QuarkusTest
@TestProfile(WebhookEventsTest.WithReceiver.class)
class WebhookEventsTest
{
	/**
	 * The profile runs in another classloader than the test: it only picks the
	 * port, shared as system property, the test starts the receiver on it
	 */
	private static final String PORT = "carledger.test.webhook-port";

	static TestReceiver receiver;

	public static class WithReceiver implements QuarkusTestProfile
	{
		@Override
		public Map<String, String> getConfigOverrides()
		{
			if (System.getProperty(PORT) == null)
			{
				try (ServerSocket socket = new ServerSocket(0))
				{
					System.setProperty(PORT, String.valueOf(socket.getLocalPort()));
				}
				catch (IOException e)
				{
					throw new UncheckedIOException(e);
				}
			}
			return Map.of("carledger.webhook.url", "http://localhost:" + System.getProperty(PORT) + "/hook",
					"carledger.webhook.secret", "s3cret",
					"carledger.webhook.backoff", "PT0.01S");
		}
	}

	@BeforeAll
	static void start() throws IOException
	{
		receiver = new TestReceiver(Integer.parseInt(System.getProperty(PORT)));
	}

	@Inject
	AccountRepository accountRepository;

	@Inject
	ObjectMapper mapper;

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();
		for (String name : List.of("peter", "alice"))
		{
			AccountEntity account = new AccountEntity();
			account.setUserId(name);
			account.setMaxCars(1);
			accountRepository.persist(account);
		}
		receiver.received.clear();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	@AfterAll
	static void stop()
	{
		receiver.close();
	}

	private JsonNode event(int index) throws Exception
	{
		long until = System.nanoTime() + Duration.ofSeconds(5).toNanos();
		while (receiver.received.size() <= index && System.nanoTime() < until)
		{
			Thread.sleep(20);
		}
		TestReceiver.Received request = receiver.received.get(index);
		assertEquals(WebhookSender.sign("s3cret", request.body()), request.header(WebhookSender.SIGNATURE));
		return mapper.readTree(request.body());
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldSendTheNewRequest() throws Exception
	{
		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "cars": 2, "reason": "For the trailer" }
						""")
				.when()
				.post("/api/user/me/car-requests")
				.then()
				.statusCode(201);

		JsonNode created = event(0);
		assertEquals("car-request.created", created.get("type").asText());
		assertEquals("peter", created.get("data").get("account").asText());
		assertEquals(2, created.get("data").get("cars").asInt());
		assertEquals("For the trailer", created.get("data").get("reason").asText());

		// a second request fails, its transaction sends nothing
		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "cars": 1 }
						""")
				.when()
				.post("/api/user/me/car-requests")
				.then()
				.statusCode(409);
		Thread.sleep(200);
		assertEquals(1, receiver.received.size());
	}

	@Inject
	CarLimitRequests carLimitRequests;

	@Transactional
	long peterRequests()
	{
		return carLimitRequests.request(accountRepository.findByIdentifier("peter"), 1, null).getId();
	}

	@Test
	@TestSecurity(user = "alice", roles = "admin")
	void shouldSendTheDecision() throws Exception
	{
		long id = peterRequests();
		event(0);

		given()
				.contentType(ContentType.JSON)
				.body("{}")
				.when()
				.post("/api/admin/car-requests/{id}/approve", id)
				.then()
				.statusCode(200);

		JsonNode decided = event(1);
		assertEquals("car-request.decided", decided.get("type").asText());
		assertEquals(id, decided.get("data").get("requestId").asLong());
		assertEquals("APPROVED", decided.get("data").get("state").asText());
		assertEquals(2, decided.get("data").get("grantedMaxCars").asInt());
		assertEquals("alice", decided.get("data").get("decidedBy").asText());
	}
}
