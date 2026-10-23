package de.codeflowwizardry.carledger.rest.car.trip;

import java.time.OffsetDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A drive of the GPS tracks without a trip, to add as a forgotten trip. Only a
 * suggestion: the user confirms or corrects it, the checks of the trip log
 * apply.
 *
 * @param distance
 *            GPS distance, km
 * @param startOdometer
 *            the highest odometer known before, or the end of the suggestion
 *            before
 * @param endOdometer
 *            the start and the GPS distance
 */
public record TripSuggestion(
		@Schema(required = true) OffsetDateTime startedAt,
		@Schema(required = true) OffsetDateTime endedAt,
		@Schema(required = true) double distance,
		@Schema(required = true) int startOdometer,
		@Schema(required = true) int endOdometer)
{
}
