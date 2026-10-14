package de.codeflowwizardry.carledger.webhook;

import java.time.OffsetDateTime;

/**
 * Data of {@link WebhookEvent#CAR_REQUEST_DECIDED}
 *
 * @param state
 *            APPROVED or REJECTED
 * @param grantedMaxCars
 *            the new limit of an approval
 * @param comment
 *            of a rejection, optional
 * @param decidedBy
 *            user name of the administrator or service account
 */
public record CarRequestDecided(long requestId, String account, String state, int cars, Integer grantedMaxCars,
		String comment, String decidedBy, OffsetDateTime decidedAt)
{
}
