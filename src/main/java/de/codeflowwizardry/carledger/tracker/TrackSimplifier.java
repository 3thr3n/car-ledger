package de.codeflowwizardry.carledger.tracker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

/**
 * Geometry of the tracks: distances on the earth and the simplification for the
 * map
 */
public final class TrackSimplifier
{
	/** mean radius of the earth in meters */
	static final double EARTH_RADIUS = 6_371_000;

	private TrackSimplifier()
	{
	}

	/**
	 * Douglas-Peucker: keeps the first and the last point and every point further
	 * than the tolerance from the line of the kept ones around it. Then a point is
	 * kept as well where the speed differs by more than the threshold from the last
	 * kept point, so braking and accelerating on a straight road stay visible, and
	 * the lowest and highest speed in between.
	 *
	 * @param tolerance
	 *            meters
	 * @param speedThreshold
	 *            km/h
	 * @return the kept points in their order
	 */
	public static List<TrackPoint> simplify(List<TrackPoint> points, double tolerance, double speedThreshold)
	{
		int size = points.size();
		if (size <= 2)
		{
			return List.copyOf(points);
		}

		boolean[] keep = new boolean[size];
		keep[0] = true;
		keep[size - 1] = true;

		// iterative, a track of hours has thousands of points
		Deque<int[]> ranges = new ArrayDeque<>();
		ranges.push(new int[] {
				0, size - 1
		});
		while (!ranges.isEmpty())
		{
			int[] range = ranges.pop();
			int first = range[0];
			int last = range[1];
			double maxDistance = -1;
			int farthest = -1;
			for (int i = first + 1; i < last; i++)
			{
				double distance = distanceToSegment(points.get(i), points.get(first), points.get(last));
				if (distance > maxDistance)
				{
					maxDistance = distance;
					farthest = i;
				}
			}
			if (farthest >= 0 && maxDistance > tolerance)
			{
				keep[farthest] = true;
				ranges.push(new int[] {
						first, farthest
				});
				ranges.push(new int[] {
						farthest, last
				});
			}
		}

		Double lastSpeed = null;
		for (int i = 0; i < size; i++)
		{
			TrackPoint point = points.get(i);
			if (!keep[i] && point.speed() != null && lastSpeed != null
					&& Math.abs(point.speed() - lastSpeed) > speedThreshold)
			{
				keep[i] = true;
			}
			if (keep[i] && point.speed() != null)
			{
				lastSpeed = point.speed();
			}
		}

		keepSpeedExtremes(points, keep, speedThreshold);

		List<TrackPoint> kept = new ArrayList<>();
		for (int i = 0; i < size; i++)
		{
			if (keep[i])
			{
				kept.add(points.get(i));
			}
		}
		return kept;
	}

	/**
	 * Keeps the lowest and the highest speed between two kept points with a speed
	 * as well, if it is beyond both of them and more than the threshold from one:
	 * braking from 50 to 20 km/h keeps 26 km/h by the threshold, the 20 km/h only
	 * by this. Repeated in the parts until nothing is kept anymore. Noise around a
	 * constant speed stays within the threshold and keeps nothing.
	 */
	private static void keepSpeedExtremes(List<TrackPoint> points, boolean[] keep, double speedThreshold)
	{
		Deque<int[]> ranges = new ArrayDeque<>();
		int previous = -1;
		for (int i = 0; i < points.size(); i++)
		{
			if (keep[i] && points.get(i).speed() != null)
			{
				if (previous >= 0)
				{
					ranges.push(new int[] {
							previous, i
					});
				}
				previous = i;
			}
		}

		while (!ranges.isEmpty())
		{
			int[] range = ranges.pop();
			int first = range[0];
			int last = range[1];
			double firstSpeed = points.get(first).speed();
			double lastSpeed = points.get(last).speed();
			double low = Math.min(firstSpeed, lastSpeed);
			double high = Math.max(firstSpeed, lastSpeed);
			int slowest = -1;
			int fastest = -1;
			for (int i = first + 1; i < last; i++)
			{
				Double speed = points.get(i).speed();
				if (speed == null)
				{
					continue;
				}
				if (speed < low && (slowest < 0 || speed < points.get(slowest).speed()))
				{
					slowest = i;
				}
				if (speed > high && (fastest < 0 || speed > points.get(fastest).speed()))
				{
					fastest = i;
				}
			}

			List<Integer> split = new ArrayList<>();
			split.add(first);
			if (slowest >= 0 && high - points.get(slowest).speed() > speedThreshold)
			{
				split.add(slowest);
			}
			if (fastest >= 0 && points.get(fastest).speed() - low > speedThreshold)
			{
				split.add(fastest);
			}
			if (split.size() == 1)
			{
				continue;
			}
			split.add(last);
			split.sort(null);
			for (int i = 1; i < split.size(); i++)
			{
				keep[split.get(i)] = true;
				ranges.push(new int[] {
						split.get(i - 1), split.get(i)
				});
			}
		}
	}

	/**
	 * @return meters between the points along the surface of the earth
	 *         (haversine)
	 */
	public static double distance(TrackPoint a, TrackPoint b)
	{
		double lat1 = Math.toRadians(a.lat());
		double lat2 = Math.toRadians(b.lat());
		double dLat = lat2 - lat1;
		double dLon = Math.toRadians(b.lon() - a.lon());
		double h = Math.pow(Math.sin(dLat / 2), 2)
				+ Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
		return 2 * EARTH_RADIUS * Math.asin(Math.min(1, Math.sqrt(h)));
	}

	/**
	 * @return meters from the point to the line between start and end, in a flat
	 *         projection around the start: precise enough for the few kilometers
	 *         between two kept points
	 */
	static double distanceToSegment(TrackPoint point, TrackPoint start, TrackPoint end)
	{
		double cosLat = Math.cos(Math.toRadians(start.lat()));
		double ex = Math.toRadians(end.lon() - start.lon()) * cosLat * EARTH_RADIUS;
		double ey = Math.toRadians(end.lat() - start.lat()) * EARTH_RADIUS;
		double px = Math.toRadians(point.lon() - start.lon()) * cosLat * EARTH_RADIUS;
		double py = Math.toRadians(point.lat() - start.lat()) * EARTH_RADIUS;

		double lengthSquared = ex * ex + ey * ey;
		double t = lengthSquared == 0 ? 0 : Math.clamp((px * ex + py * ey) / lengthSquared, 0, 1);
		return Math.hypot(px - t * ex, py - t * ey);
	}
}
