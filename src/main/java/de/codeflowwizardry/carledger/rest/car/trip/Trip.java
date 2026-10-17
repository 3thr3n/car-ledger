package de.codeflowwizardry.carledger.rest.car.trip;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.TripEntity;
import de.codeflowwizardry.carledger.data.TripReview;
import de.codeflowwizardry.carledger.data.TripType;

/**
 * A trip, odometers and distance in km
 *
 * @param startedAt
 *            time of the device
 * @param distance
 *            missing while the trip is open
 * @param reviewReason
 *            a trip from the offline queue that doesn't fit the other data
 * @param restored
 *            created by the restore of a backup
 * @param createdAt
 *            when the server received the start
 * @param finishedAt
 *            when the server received the finish
 */
public record Trip(
		@Schema(required = true) UUID id,
		@Schema(required = true) long carId,
		@Schema(required = true) TripType type,
		@Schema(required = true) int startOdometer,
		Integer endOdometer,
		Integer distance,
		@Schema(required = true) OffsetDateTime startedAt,
		OffsetDateTime endedAt,
		String destination,
		String purpose,
		String businessPartner,
		TripReview reviewReason,
		@Schema(required = true) boolean restored,
		@Schema(required = true) OffsetDateTime createdAt,
		OffsetDateTime finishedAt)
{
	public static Trip convert(TripEntity entity)
	{
		return new Trip(entity.getId(), entity.getCar().getId(), entity.getType(), entity.getStartOdometer(),
				entity.getEndOdometer(), entity.getDistance(), entity.getStartedAt(), entity.getEndedAt(),
				entity.getDestination(), entity.getPurpose(), entity.getBusinessPartner(), entity.getReviewReason(),
				entity.isRestored(), entity.getCreatedAt(), entity.getFinishedAt());
	}
}
