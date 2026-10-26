package de.codeflowwizardry.carledger.webhook;

import java.time.OffsetDateTime;

import de.codeflowwizardry.carledger.data.CarAccessEntity;

/**
 * Data of the events of an access to a car:
 * {@link WebhookEvent#CAR_ACCESS_INVITED},
 * {@link WebhookEvent#CAR_ACCESS_ANSWERED},
 * {@link WebhookEvent#CAR_ACCESS_CHANGED} and
 * {@link WebhookEvent#CAR_ACCESS_REVOKED}
 *
 * @param owner
 *            user name of the owner of the car
 * @param account
 *            user name of the invited account, missing while it doesn't exist
 * @param invitee
 *            user name or e-mail address of an account that doesn't exist yet
 * @param state
 *            PENDING, ACCEPTED or DECLINED
 * @param validFrom
 *            start of the access, missing for from now on
 * @param validUntil
 *            end of the access (exclusive), missing for without an end
 * @param by
 *            user name of the account that did it: the owner, or the invited
 *            one for an answer
 */
public record CarAccessEvent(long accessId, long carId, String car, String owner, String account, String invitee,
		String role, String state, OffsetDateTime validFrom, OffsetDateTime validUntil, String by)
{
	public static CarAccessEvent of(CarAccessEntity access, String by)
	{
		return new CarAccessEvent(access.getId(), access.getCar().getId(), access.getCar().getName(),
				access.getCar().getUser().getUserId(),
				access.getAccount() == null ? null : access.getAccount().getUserId(), access.getInvitee(),
				access.getRole().name(), access.getState().name(), access.getValidFrom(), access.getValidUntil(),
				by);
	}
}
