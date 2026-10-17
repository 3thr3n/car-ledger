package de.codeflowwizardry.carledger.webhook;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The JSON body of a webhook
 *
 * @param version
 *            of the payload, raised with incompatible changes
 * @param type
 *            e.g. car-request.created
 * @param id
 *            unique per event, also in the header X-CarLedger-Delivery; the
 *            same
 *            for its retries
 * @param data
 *            depends on the type
 */
public record WebhookMessage(int version, String type, String id, OffsetDateTime occurredAt, Object data)
{
	public static final int VERSION = 1;

	public static WebhookMessage of(WebhookEvent event, Object data)
	{
		return new WebhookMessage(VERSION, event.type(), UUID.randomUUID().toString(), OffsetDateTime.now(), data);
	}
}
