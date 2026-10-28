package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

/**
 * Six drives on a curved road (#526): the shared line brings them together, the
 * simplification of each drive on its own takes them apart again by about its
 * tolerance. So the detail of a zoomed-in map needs the tolerance of a pixel.
 */
class RepeatedDrivesTest
{
	private static final double LAT_PER_METER = 1 / 111_195.0;
	private static final double LON_PER_METER = LAT_PER_METER / Math.cos(Math.toRadians(50));
	private static final double RADIUS = 400;

	/** x, y and the direction at s meters of an arc of 3 km */
	private static double[] road(double s)
	{
		double a = s / RADIUS;
		return new double[] {
				RADIUS * Math.sin(a), RADIUS - RADIUS * Math.cos(a), Math.cos(a), Math.sin(a)
		};
	}

	/**
	 * 80 km/h, a point every 2 s at another phase, on one of two lanes 3.5 m
	 * apart with an offset of the GPS of each drive and noise of each point
	 */
	private static List<TrackPoint> drive(int index, Random random)
	{
		OffsetDateTime start = OffsetDateTime.parse("2026-10-20T08:00:00Z").plusHours(index);
		double offset = (index % 2 == 0 ? 1.75 : -1.75) + random.nextGaussian() * 2;
		List<TrackPoint> points = new ArrayList<>();
		for (double t = random.nextDouble() * 2; t * 22 < 3000; t += 2)
		{
			double[] p = road(t * 22);
			double x = p[0] - p[3] * offset + random.nextGaussian() * 1.5;
			double y = p[1] + p[2] * offset + random.nextGaussian() * 1.5;
			points.add(new TrackPoint(start.plusNanos((long) (t * 1e9)), 50 + y * LAT_PER_METER,
					10 + x * LON_PER_METER, 80.0));
		}
		return points;
	}

	private static double[] xy(TrackPoint point)
	{
		return new double[] {
				(point.lon() - 10) / LON_PER_METER, (point.lat() - 50) / LAT_PER_METER
		};
	}

	private static double distance(double[] p, double[] a, double[] b)
	{
		double ex = b[0] - a[0];
		double ey = b[1] - a[1];
		double length = ex * ex + ey * ey;
		double t = length == 0 ? 0 : Math.clamp(((p[0] - a[0]) * ex + (p[1] - a[1]) * ey) / length, 0, 1);
		return Math.hypot(p[0] - a[0] - t * ex, p[1] - a[1] - t * ey);
	}

	/** meters, the most a line is away from the other, away from the ends */
	private static double gap(List<TrackPoint> line, List<TrackPoint> other)
	{
		double max = 0;
		for (int i = 1; i < line.size(); i++)
		{
			double[] p = xy(line.get(i - 1));
			double[] q = xy(line.get(i));
			int steps = (int) Math.ceil(Math.hypot(q[0] - p[0], q[1] - p[1]) / 5);
			for (int k = 0; k <= steps; k++)
			{
				double f = steps == 0 ? 0 : (double) k / steps;
				double[] s = {
						p[0] + (q[0] - p[0]) * f, p[1] + (q[1] - p[1]) * f
				};
				if (s[0] < 100 || s[0] > 2300)
				{
					continue;
				}
				double nearest = Double.MAX_VALUE;
				for (int j = 1; j < other.size(); j++)
				{
					nearest = Math.min(nearest, distance(s, xy(other.get(j - 1)), xy(other.get(j))));
				}
				max = Math.max(max, nearest);
			}
		}
		return max;
	}

	private static double maxGap(double tolerance)
	{
		Random random = new Random(7);
		List<List<TrackPoint>> drives = new ArrayList<>();
		for (int i = 0; i < 6; i++)
		{
			drives.add(drive(i, random));
		}
		SharedLine shared = new SharedLine(Duration.ofMinutes(5));
		drives.forEach(drive -> drive.forEach(shared::add));
		List<List<TrackPoint>> lines = drives.stream()
				.map(drive -> TrackSimplifier.simplify(shared.apply(drive), tolerance, Double.POSITIVE_INFINITY))
				.toList();
		double max = 0;
		for (List<TrackPoint> line : lines)
		{
			for (List<TrackPoint> other : lines)
			{
				if (line != other)
				{
					max = Math.max(max, gap(line, other));
				}
			}
		}
		return max;
	}

	@Test
	void shouldShowOneLineWhenZoomedIn()
	{
		// the detail of a viewport at zoom 16: less than a pixel each
		double tolerance = TrackService.tolerance(Duration.ofDays(3), new Viewport(9.9, 49.9, 10.1, 50.1, 16));
		double gap = maxGap(tolerance);

		assertTrue(gap < 6, "gap " + gap);
	}

	@Test
	void shouldDriftApartWithTheToleranceOfTheWholeRange()
	{
		// why the detail is needed: 3 days are simplified with 15 m
		assertTrue(maxGap(TrackService.tolerance(Duration.ofDays(3))) > 12);
	}
}
