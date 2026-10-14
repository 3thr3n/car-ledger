package de.codeflowwizardry.carledger.webhook;

import java.time.OffsetDateTime;

/**
 * Data of {@link WebhookEvent#CAR_REQUEST_CREATED}
 *
 * @param account
 *            user name of the account
 * @param cars
 *            how many cars more
 * @param maxCars
 *            the limit of the account
 * @param reason
 *            optional
 */
public record CarRequestCreated(long requestId, String account, int cars, int maxCars, String reason,
		OffsetDateTime createdAt)
{
}
