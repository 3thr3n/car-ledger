package de.codeflowwizardry.carledger.rest.car.track;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Points of a track without a gap, compact for the map: a busy month has tens
 * of thousands of them
 *
 * @param start
 *            time of the first point
 * @param points
 *            each <code>[lon, lat, seconds, speed]</code>: degrees with 6
 *            decimals (about 0.1 m), seconds since the start with 1 decimal,
 *            km/h with 1 decimal or <code>null</code> without a speed
 */
public record TrackSegment(
		@Schema(required = true) OffsetDateTime start,
		@Schema(required = true) Double[][] points)
{
	/**
	 * @param points
	 *            at least one, in the order of time
	 */
	public static TrackSegment of(List<TrackPoint> points)
	{
		OffsetDateTime start = points.getFirst().ts();
		Double[][] compact = new Double[points.size()][];
		for (int i = 0; i < compact.length; i++)
		{
			TrackPoint point = points.get(i);
			compact[i] = new Double[] {
					round(point.lon(), 1e6), round(point.lat(), 1e6),
					round(Duration.between(start, point.ts()).toMillis() / 1000.0, 10),
					point.speed() == null ? null : round(point.speed(), 10)
			};
		}
		return new TrackSegment(start, compact);
	}

	/** degrees with 6 decimals, as the frequency lines */
	public static double round(double value, double factor)
	{
		return Math.round(value * factor) / factor;
	}
}
