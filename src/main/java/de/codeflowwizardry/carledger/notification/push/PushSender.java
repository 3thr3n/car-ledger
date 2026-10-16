package de.codeflowwizardry.carledger.notification.push;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

/**
 * Sends the push messages after the commit: encrypted for the browser (RFC
 * 8291), signed with the key of the server (VAPID), asynchronously with a few
 * retries. A subscription the push service does not know anymore is removed. A
 * failing push service never fails the request of the user.
 */
@ApplicationScoped
public class PushSender
{
	private static final Logger LOG = LoggerFactory.getLogger(PushSender.class);

	private final PushConfig config;
	private final VapidKeys keys;
	private final PushSubscriptionRepository repository;
	private final HttpClient client;

	@Inject
	public PushSender(PushConfig config, VapidKeys keys, PushSubscriptionRepository repository)
	{
		this(config, keys, repository, HttpClient.newBuilder().connectTimeout(config.timeout()).build());
	}

	PushSender(PushConfig config, VapidKeys keys, PushSubscriptionRepository repository, HttpClient client)
	{
		this.config = config;
		this.keys = keys;
		this.repository = repository;
		this.client = client;
	}

	void onPushes(@Observes(during = TransactionPhase.AFTER_SUCCESS) OutgoingPushes outgoing)
	{
		for (PushMessage message : outgoing.messages())
		{
			send(outgoing.event(), message);
		}
	}

	/**
	 * @return completes after the last attempt, never exceptionally
	 */
	CompletableFuture<Boolean> send(String event, PushMessage message)
	{
		if (!PushEndpoints.isAllowed(message.endpoint(), config.allowedHosts()))
		{
			// saved before the check, or the allowed hosts changed
			LOG.warn("Push {} to subscription {} not sent: the endpoint is no known push service", event,
					message.subscriptionId());
			return CompletableFuture.completedFuture(false);
		}
		HttpRequest request;
		try
		{
			URI endpoint = URI.create(message.endpoint());
			byte[] body = WebPushCrypto.encrypt(message.payload().getBytes(StandardCharsets.UTF_8),
					Base64.getUrlDecoder().decode(message.p256dh()), Base64.getUrlDecoder().decode(message.auth()));
			request = HttpRequest.newBuilder(endpoint)
					.timeout(config.timeout())
					.header("TTL", String.valueOf(config.ttl().toSeconds()))
					.header("Urgency", "normal")
					.header("Content-Type", "application/octet-stream")
					.header("Content-Encoding", "aes128gcm")
					.header("Authorization", keys.authorization(endpoint, config.subject(), Instant.now()))
					.POST(HttpRequest.BodyPublishers.ofByteArray(body))
					.build();
		}
		catch (RuntimeException e)
		{
			LOG.warn("Push {} to subscription {} cannot be sent: {}", event, message.subscriptionId(), e.toString());
			return CompletableFuture.completedFuture(false);
		}
		return attempt(request, event, message, 0);
	}

	private CompletableFuture<Boolean> attempt(HttpRequest request, String event, PushMessage message, int retry)
	{
		// the body is not read: only the status counts, nothing of the answer reaches
		// the log
		return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
				.handle((response, error) -> {
					int status = error == null ? response.statusCode() : 0;
					if (status / 100 == 2)
					{
						return CompletableFuture.completedFuture(true);
					}
					if (status == 404 || status == 410)
					{
						// the browser unsubscribed or the subscription expired
						remove(message);
						return CompletableFuture.completedFuture(false);
					}
					String problem = error != null ? error.getClass().getSimpleName() : "status " + status;
					boolean temporary = status == 0 || status == 429 || status >= 500;
					if (!temporary || retry >= config.retries())
					{
						LOG.warn("Push {} to subscription {} not delivered after {} attempts: {}", event,
								message.subscriptionId(), retry + 1, problem);
						return CompletableFuture.completedFuture(false);
					}
					Duration wait = config.backoff().multipliedBy((long) Math.pow(4, retry));
					LOG.info("Push {} to subscription {} failed ({}), retry in {}", event, message.subscriptionId(),
							problem, wait);
					return CompletableFuture.supplyAsync(() -> null,
							CompletableFuture.delayedExecutor(wait.toMillis(), TimeUnit.MILLISECONDS))
							.thenCompose(ignored -> attempt(request, event, message, retry + 1));
				})
				.thenCompose(result -> result);
	}

	private void remove(PushMessage message)
	{
		try
		{
			QuarkusTransaction.requiringNew().run(() -> repository.delete("id", message.subscriptionId()));
			LOG.info("Push subscription {} is gone, removed", message.subscriptionId());
		}
		catch (RuntimeException e)
		{
			LOG.warn("Push subscription {} could not be removed: {}", message.subscriptionId(), e.toString());
		}
	}
}
