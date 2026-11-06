package de.codeflowwizardry.carledger.rest.car.track;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
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
 *            km/h with 1 decimal or <code>null</code> without a speed; with
 *            the g-forces also <code>[accel, brake, lateral, imu]</code>: g
 *            with 2 decimals, imu 1 or 0, all <code>null</code> without
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
		return of(points, false);
	}

	/**
	 * @param points
	 *            at least one, in the order of time
	 * @param gForces
	 *            with the four values of the g-forces
	 */
	public static TrackSegment of(List<TrackPoint> points, boolean gForces)
	{
		OffsetDateTime start = points.getFirst().ts();
		Double[][] compact = new Double[points.size()][];
		for (int i = 0; i < compact.length; i++)
		{
			TrackPoint point = points.get(i);
			Double[] values = {
					round(point.lon(), 1e6), round(point.lat(), 1e6),
					round(Duration.between(start, point.ts()).toMillis() / 1000.0, 10),
					point.speed() == null ? null : round(point.speed(), 10)
			};
			if (gForces)
			{
				GForces g = point.g();
				values = Arrays.copyOf(values, 8);
				if (g != null)
				{
					values[4] = g.accel() == null ? null : round(g.accel(), 100);
					values[5] = g.brake() == null ? null : round(g.brake(), 100);
					values[6] = g.lateral() == null ? null : round(g.lateral(), 100);
					values[7] = g.imu() ? 1.0 : 0.0;
				}
			}
			compact[i] = values;
		}
		return new TrackSegment(start, compact);
	}

	/** degrees with 6 decimals, as the frequency lines */
	public static double round(double value, double factor)
	{
		return Math.round(value * factor) / factor;
	}
}
