package de.codeflowwizardry.carledger.rest.car.track;

import java.time.OffsetDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A drive of the GPS track, from a stop to the next
 *
 * @param start
 *            the time it left
 * @param end
 *            the time it arrived, the point where it stopped
 * @param distance
 *            GPS distance in km
 * @param from
 *            name of the named place it left from, missing without one
 * @param to
 *            name of the named place it arrived at, missing without one
 */
public record TrackDrive(
		@Schema(required = true) OffsetDateTime start,
		@Schema(required = true) OffsetDateTime end,
		@Schema(required = true) double distance,
		String from,
		String to)
{
}
