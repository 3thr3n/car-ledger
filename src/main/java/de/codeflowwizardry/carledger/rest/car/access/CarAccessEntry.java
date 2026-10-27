package de.codeflowwizardry.carledger.rest.car.access;

import java.time.OffsetDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.CarAccessEntity;
import de.codeflowwizardry.carledger.data.CarAccessState;
import de.codeflowwizardry.carledger.data.CarRole;

/**
 * An access to a car or an invitation, as the owner sees it
 *
 * @param account
 *            user name of the account, missing while it doesn't exist
 * @param invitee
 *            user name or e-mail address of an account that doesn't exist yet
 * @param validFrom
 *            start, from the acceptance on without
 * @param validUntil
 *            end (exclusive), without an end without
 */
public record CarAccessEntry(
		@Schema(required = true) long id,
		String account,
		String invitee,
		@Schema(required = true) CarRole role,
		@Schema(required = true) CarAccessState state,
		OffsetDateTime validFrom,
		OffsetDateTime validUntil,
		@Schema(required = true) OffsetDateTime createdAt)
{
	public static CarAccessEntry convert(CarAccessEntity entity)
	{
		return new CarAccessEntry(entity.getId(),
				entity.getAccount() == null ? null : entity.getAccount().getUserId(), entity.getInvitee(),
				entity.getRole(), entity.getState(), entity.getValidFrom(), entity.getValidUntil(),
				entity.getCreatedAt());
	}
}
