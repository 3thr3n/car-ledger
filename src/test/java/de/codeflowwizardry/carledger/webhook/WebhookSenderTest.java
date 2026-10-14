package de.codeflowwizardry.carledger.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

class WebhookSenderTest
{
	private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	private TestReceiver receiver;

	@BeforeEach
	void start() throws Exception
	{
		receiver = new TestReceiver();
	}

	@AfterEach
	void stop()
	{
		receiver.close();
	}

	private WebhookSender sender(Optional<URI> url, Optional<String> secret, Optional<List<String>> events)
	{
		WebhookConfig config = new WebhookConfig() {
			@Override
			public Optional<URI> url()
			{
				return url;
			}

			@Override
			public Optional<String> secret()
			{
				return secret;
			}

			@Override
			public Optional<List<String>> events()
			{
				return events;
			}

			@Override
			public int retries()
			{
				return 2;
			}

			@Override
			public Duration backoff()
			{
				return Duration.ofMillis(10);
			}

			@Override
			public Duration timeout()
			{
				return Duration.ofSeconds(5);
			}
		};
		return new WebhookSender(config, mapper, HttpClient.newHttpClient());
	}

	private static WebhookMessage created()
	{
		return WebhookMessage.of(WebhookEvent.CAR_REQUEST_CREATED,
				new CarRequestCreated(7, "peter", 2, 1, "For the trailer", OffsetDateTime.now()));
	}

	@Test
	void shouldSendASignedEvent() throws Exception
	{
		WebhookMessage message = created();
		assertTrue(sender(Optional.of(receiver.url()), Optional.of("s3cret"), Optional.empty()).send(message).get());

		assertEquals(1, receiver.received.size());
		TestReceiver.Received request = receiver.received.getFirst();
		assertEquals(WebhookSender.sign("s3cret", request.body()), request.header(WebhookSender.SIGNATURE));
		assertEquals("car-request.created", request.header(WebhookSender.EVENT));
		assertEquals(message.id(), request.header(WebhookSender.DELIVERY));

		JsonNode body = mapper.readTree(request.body());
		assertEquals(1, body.get("version").asInt());
		assertEquals("car-request.created", body.get("type").asText());
		assertEquals("peter", body.get("data").get("account").asText());
		assertEquals(2, body.get("data").get("cars").asInt());
		assertTrue(body.get("occurredAt").isTextual());
	}

	@Test
	void shouldSignLikeOpenssl()
	{
		// printf '{}' | openssl dgst -sha256 -hmac s3cret
		assertEquals("sha256=adbde1ce40c89c14215687d5d762a47df6dfaefcfad61e2e86718ffc8498571b",
				WebhookSender.sign("s3cret", "{}".getBytes()));
	}

	@Test
	void shouldRetryWithTheSameDelivery() throws Exception
	{
		receiver.failFirst.set(2);

		assertTrue(sender(Optional.of(receiver.url()), Optional.empty(), Optional.empty()).send(created()).get());

		assertEquals(3, receiver.received.size());
		assertEquals(receiver.received.get(0).header(WebhookSender.DELIVERY),
				receiver.received.get(2).header(WebhookSender.DELIVERY));
		// without a secret not signed
		assertNull(receiver.received.getFirst().header(WebhookSender.SIGNATURE));
	}

	@Test
	void shouldGiveUpAfterTheRetries() throws Exception
	{
		receiver.failFirst.set(10);

		assertFalse(sender(Optional.of(receiver.url()), Optional.empty(), Optional.empty()).send(created()).get());
		assertEquals(3, receiver.received.size());
	}

	@Test
	void shouldGiveUpWithoutAReceiver() throws Exception
	{
		URI closed = receiver.url();
		receiver.close();

		assertFalse(sender(Optional.of(closed), Optional.empty(), Optional.empty()).send(created()).get());
	}

	@Test
	void shouldSendNothingWithoutAUrlOrForOtherEvents() throws Exception
	{
		assertFalse(sender(Optional.empty(), Optional.of("s3cret"), Optional.empty()).send(created()).get());
		assertFalse(sender(Optional.of(receiver.url()), Optional.empty(), Optional.of(List.of("car-request.decided")))
				.send(created()).get());
		assertEquals(0, receiver.received.size());
	}
}
