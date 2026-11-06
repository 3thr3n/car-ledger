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
 * @param g
 *            the g-forces since the point before, missing without
 */
public record TrackPoint(
		@Schema(required = true) OffsetDateTime ts,
		@Schema(required = true) double lat,
		@Schema(required = true) double lon,
		Double speed,
		GForces g)
{
	/** without g-forces, e.g. a position */
	public TrackPoint(OffsetDateTime ts, double lat, double lon, Double speed)
	{
		this(ts, lat, lon, speed, null);
	}

	/** the same point with other g-forces */
	public TrackPoint with(GForces forces)
	{
		return new TrackPoint(ts, lat, lon, speed, forces);
	}
}
