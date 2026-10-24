package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

class TrackSimplifierTest
{
	private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-20T08:00:00Z");
	/** degrees of latitude per meter */
	private static final double LAT_PER_METER = 1 / 111_195.0;

	/** a point the given meters north and east of 50°N 10°E */
	private static TrackPoint point(int second, double north, double east, Double speed)
	{
		double lat = 50 + north * LAT_PER_METER;
		double lon = 10 + east * LAT_PER_METER / Math.cos(Math.toRadians(50));
		return new TrackPoint(START.plusSeconds(second), lat, lon, speed);
	}

	/** points every 10 m to the north at the same speed */
	private static List<TrackPoint> straight(int count)
	{
		List<TrackPoint> points = new ArrayList<>();
		for (int i = 0; i < count; i++)
		{
			points.add(point(i, i * 10, 0, 50.0));
		}
		return points;
	}

	@Test
	void shouldKeepOnlyStartAndEndOfAStraightLine()
	{
		List<TrackPoint> points = straight(100);

		assertEquals(List.of(points.getFirst(), points.getLast()), TrackSimplifier.simplify(points, 5, 10));
	}

	@Test
	void shouldKeepACorner()
	{
		List<TrackPoint> points = new ArrayList<>(straight(50));
		for (int i = 1; i < 50; i++)
		{
			points.add(point(50 + i, 490, i * 10, 50.0));
		}

		assertEquals(List.of(points.getFirst(), points.get(49), points.getLast()),
				TrackSimplifier.simplify(points, 5, 10));
	}

	@Test
	void shouldDropDeviationsWithinTheTolerance()
	{
		List<TrackPoint> points = new ArrayList<>();
		for (int i = 0; i < 100; i++)
		{
			points.add(point(i, i * 10, i % 2 == 0 ? 0 : 3, 50.0));
		}

		assertEquals(2, TrackSimplifier.simplify(points, 5, 10).size());
	}

	@Test
	void shouldKeepASpeedChangeOnAStraightLine()
	{
		List<TrackPoint> points = straight(100);
		// braking from 50 to 20 km/h and accelerating again
		for (int i = 40; i < 60; i++)
		{
			points.set(i, point(i, i * 10, 0, i < 50 ? 50.0 - (i - 39) * 3 : 20.0 + (i - 49) * 3));
		}

		List<TrackPoint> kept = TrackSimplifier.simplify(points, 5, 10);

		assertEquals(points.getFirst(), kept.getFirst());
		assertEquals(points.getLast(), kept.getLast());
		// each kept point is more than 10 km/h from the one before: 38 and 26 braking,
		// 38 and 50 accelerating
		assertEquals(List.of(50.0, 38.0, 26.0, 38.0, 50.0, 50.0),
				kept.stream().map(TrackPoint::speed).toList());
	}

	@Test
	void shouldIgnoreMissingSpeeds()
	{
		List<TrackPoint> points = new ArrayList<>(straight(10));
		points.set(5, point(5, 50, 0, null));

		assertEquals(2, TrackSimplifier.simplify(points, 5, 10).size());
	}

	@Test
	void shouldKeepShortTracks()
	{
		List<TrackPoint> points = straight(2);

		assertEquals(points, TrackSimplifier.simplify(points, 5, 10));
		assertEquals(List.of(points.getFirst()), TrackSimplifier.simplify(points.subList(0, 1), 5, 10));
	}

	@Test
	void shouldMeasureTheDistanceOnTheEarth()
	{
		TrackPoint a = new TrackPoint(START, 50, 10, null);
		TrackPoint b = new TrackPoint(START, 51, 10, null);
		TrackPoint c = new TrackPoint(START, 50, 11, null);

		assertEquals(111_195, TrackSimplifier.distance(a, b), 1);
		// a degree of longitude is shorter at 50°N
		assertEquals(71_474, TrackSimplifier.distance(a, c), 50);
	}
}
