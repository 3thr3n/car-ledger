package de.codeflowwizardry.carledger.rest.car.access;

import java.time.OffsetDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.CarAccessEntity;
import de.codeflowwizardry.carledger.data.CarRole;

/**
 * An invitation to a car the user hasn't answered yet
 *
 * @param owner
 *            user name of the owner of the car
 * @param validFrom
 *            start, from the acceptance on without
 * @param validUntil
 *            end (exclusive), without an end without
 */
public record CarInvitation(
		@Schema(required = true) long id,
		@Schema(required = true) long carId,
		@Schema(required = true) String car,
		@Schema(required = true) String owner,
		@Schema(required = true) CarRole role,
		OffsetDateTime validFrom,
		OffsetDateTime validUntil,
		@Schema(required = true) OffsetDateTime createdAt)
{
	public static CarInvitation convert(CarAccessEntity entity)
	{
		return new CarInvitation(entity.getId(), entity.getCar().getId(), entity.getCar().getName(),
				entity.getCar().getUser().getUserId(), entity.getRole(), entity.getValidFrom(), entity.getValidUntil(),
				entity.getCreatedAt());
	}
}
