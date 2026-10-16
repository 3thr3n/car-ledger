package de.codeflowwizardry.carledger.webhook;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

/**
 * Sends the events to the configured URL: signed, asynchronously, with a few
 * retries. A failing or missing receiver never fails the request of the user,
 * the errors are only logged.
 */
@ApplicationScoped
public class WebhookSender
{
	private static final Logger LOG = LoggerFactory.getLogger(WebhookSender.class);

	static final String SIGNATURE = "X-CarLedger-Signature";
	static final String EVENT = "X-CarLedger-Event";
	static final String DELIVERY = "X-CarLedger-Delivery";

	private final WebhookConfig config;
	private final ObjectMapper mapper;
	private final HttpClient client;

	@Inject
	public WebhookSender(WebhookConfig config, ObjectMapper mapper)
	{
		this(config, mapper, HttpClient.newBuilder().connectTimeout(config.timeout()).build());
	}

	WebhookSender(WebhookConfig config, ObjectMapper mapper, HttpClient client)
	{
		this.config = config;
		this.mapper = mapper;
		this.client = client;
		config.events().ifPresent(events -> events.stream()
				.filter(type -> WebhookEvent.ofType(type).isEmpty())
				.forEach(type -> LOG.warn("Unknown webhook event {} in carledger.webhook.events", type)));
	}

	/**
	 * Sends the event once the transaction succeeded, at once without one
	 *
	 * @return completes after the last attempt, never exceptionally
	 */
	void onEvent(@Observes(during = TransactionPhase.AFTER_SUCCESS) WebhookMessage message)
	{
		send(message);
	}

	CompletableFuture<Boolean> send(WebhookMessage message)
	{
		if (config.url().isEmpty() || !isSelected(message.type()))
		{
			return CompletableFuture.completedFuture(false);
		}
		byte[] body;
		try
		{
			body = mapper.writeValueAsBytes(message);
		}
		catch (JsonProcessingException e)
		{
			LOG.error("Webhook {} {} cannot be written", message.type(), message.id(), e);
			return CompletableFuture.completedFuture(false);
		}

		HttpRequest.Builder request = HttpRequest.newBuilder(config.url().get())
				.timeout(config.timeout())
				.header("Content-Type", "application/json")
				.header(EVENT, message.type())
				.header(DELIVERY, message.id())
				.POST(HttpRequest.BodyPublishers.ofByteArray(body));
		config.secret().filter(secret -> !secret.isBlank())
				.ifPresent(secret -> request.header(SIGNATURE, sign(secret, body)));
		return attempt(request.build(), message, 0);
	}

	private CompletableFuture<Boolean> attempt(HttpRequest request, WebhookMessage message, int retry)
	{
		return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
				.handle((response, error) -> {
					if (error == null && response.statusCode() / 100 == 2)
					{
						return CompletableFuture.completedFuture(true);
					}
					String problem = error != null ? error.toString() : "status " + response.statusCode();
					if (retry >= config.retries())
					{
						LOG.warn("Webhook {} {} not delivered after {} attempts: {}", message.type(), message.id(),
								retry + 1, problem);
						return CompletableFuture.completedFuture(false);
					}
					Duration wait = config.backoff().multipliedBy((long) Math.pow(4, retry));
					LOG.info("Webhook {} {} failed ({}), retry in {}", message.type(), message.id(), problem, wait);
					return CompletableFuture.supplyAsync(() -> null,
							CompletableFuture.delayedExecutor(wait.toMillis(), TimeUnit.MILLISECONDS))
							.thenCompose(ignored -> attempt(request, message, retry + 1));
				})
				.thenCompose(result -> result);
	}

	private boolean isSelected(String type)
	{
		return config.events().map(events -> events.contains(type)).orElse(true);
	}

	/**
	 * @return sha256=hex of the HMAC-SHA256 of the body with the secret
	 */
	static String sign(String secret, byte[] body)
	{
		try
		{
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
		}
		catch (NoSuchAlgorithmException | InvalidKeyException e)
		{
			throw new IllegalStateException("HMAC-SHA256 is not available", e);
		}
	}
}
