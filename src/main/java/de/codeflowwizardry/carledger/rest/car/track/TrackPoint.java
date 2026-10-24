package de.codeflowwizardry.carledger.rest.car.track;

import java.time.OffsetDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * A point of a track
 *
 * @param ts
 *            GPS time
 * @param speed
 *            km/h, missing when the tracker didn't send it
 */
public record TrackPoint(
		@Schema(required = true) OffsetDateTime ts,
		@Schema(required = true) double lat,
		@Schema(required = true) double lon,
		Double speed)
{
}
