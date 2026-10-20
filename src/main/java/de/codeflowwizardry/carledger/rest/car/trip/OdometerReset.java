package de.codeflowwizardry.carledger.rest.car.trip;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.OdometerResetEntity;

/**
 * An odometer reset of the trip log, odometers in km
 *
 * @param resetAt
 *            time of the device
 * @param odometer
 *            the new reading, the base from then on
 * @param odometerBefore
 *            the last reading before, if it was still shown
 * @param createdBy
 *            the account that added it
 * @param restored
 *            created by the restore of a backup
 * @param createdAt
 *            when the server received it
 * @param deletedAt
 *            when it was deleted, it stays listed and doesn't count
 */
public record OdometerReset(
		@Schema(required = true) UUID id,
		@Schema(required = true) long carId,
		@Schema(required = true) OffsetDateTime resetAt,
		@Schema(required = true) int odometer,
		Integer odometerBefore,
		@Schema(required = true) String reason,
		@Schema(required = true) String createdBy,
		@Schema(required = true) boolean restored,
		@Schema(required = true) OffsetDateTime createdAt,
		OffsetDateTime deletedAt,
		String deletedBy,
		String deleteReason)
{
	public static OdometerReset convert(OdometerResetEntity entity)
	{
		return new OdometerReset(entity.getId(), entity.getCar().getId(), entity.getResetAt(), entity.getOdometer(),
				entity.getOdometerBefore(), entity.getReason(), entity.getCreatedBy(), entity.isRestored(),
				entity.getCreatedAt(), entity.getDeletedAt(), entity.getDeletedBy(), entity.getDeleteReason());
	}
}
