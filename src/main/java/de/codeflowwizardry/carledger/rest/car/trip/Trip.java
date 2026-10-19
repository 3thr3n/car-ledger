package de.codeflowwizardry.carledger.rest.car.trip;

import java.time.Duration;
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
 * @param deletedAt
 *            when the trip was deleted, it stays listed and doesn't count
 * @param deletedBy
 *            the account that deleted it
 * @param deleteReason
 *            why it was deleted
 * @param enteredLater
 *            the server received the start or the finish more than
 *            {@link #ENTERED_LATER} after the time of the device: entered
 *            afterwards, not on the road
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
		OffsetDateTime finishedAt,
		OffsetDateTime deletedAt,
		String deletedBy,
		String deleteReason,
		@Schema(required = true) boolean enteredLater)
{
	/**
	 * a request of the offline queue arrives later as well, but was made on the
	 * road
	 */
	public static final Duration ENTERED_LATER = Duration.ofHours(2);

	public static Trip convert(TripEntity entity)
	{
		return new Trip(entity.getId(), entity.getCar().getId(), entity.getType(), entity.getStartOdometer(),
				entity.getEndOdometer(), entity.getDistance(), entity.getStartedAt(), entity.getEndedAt(),
				entity.getDestination(), entity.getPurpose(), entity.getBusinessPartner(), entity.getReviewReason(),
				entity.isRestored(), entity.getCreatedAt(), entity.getFinishedAt(), entity.getDeletedAt(),
				entity.getDeletedBy(), entity.getDeleteReason(), enteredLater(entity));
	}

	static boolean enteredLater(TripEntity entity)
	{
		return later(entity.getStartedAt(), entity.getCreatedAt())
				|| entity.getFinishedAt() != null && later(entity.getEndedAt(), entity.getFinishedAt());
	}

	private static boolean later(OffsetDateTime device, OffsetDateTime received)
	{
		return Duration.between(device, received).compareTo(ENTERED_LATER) > 0;
	}
}
