package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

/**
 * Drives with a tracker sending every second at 36 km/h, around 50°N 10°E
 */
class SharedLineTest
{
	private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-20T08:00:00Z");
	/** degrees of latitude per meter */
	private static final double LAT_PER_METER = 1 / 111_195.0;
	private static final double LON_PER_METER = LAT_PER_METER / Math.cos(Math.toRadians(50));

	/** seconds since {@link #START} */
	private int second;

	private TrackPoint point(double north, double east)
	{
		return new TrackPoint(START.plusSeconds(second++), 50 + north * LAT_PER_METER, 10 + east * LON_PER_METER,
				42.0);
	}

	/** to the north or the south, 10 m per second */
	private List<TrackPoint> northSouth(double fromNorth, double toNorth, double east)
	{
		List<TrackPoint> drive = new ArrayList<>();
		double step = fromNorth < toNorth ? 10 : -10;
		for (double north = fromNorth; step > 0 ? north <= toNorth : north >= toNorth; north += step)
		{
			drive.add(point(north, east));
		}
		// the next drive an hour later
		second += 3600;
		return drive;
	}

	private List<TrackPoint> eastWest(double north, double fromEast, double toEast)
	{
		List<TrackPoint> drive = new ArrayList<>();
		for (double east = fromEast; east <= toEast; east += 10)
		{
			drive.add(point(north, east));
		}
		second += 3600;
		return drive;
	}

	private static SharedLine shared(List<List<TrackPoint>> drives)
	{
		SharedLine shared = new SharedLine(Duration.ofMinutes(5));
		drives.forEach(drive -> drive.forEach(shared::add));
		return shared;
	}

	private static double east(TrackPoint point)
	{
		return (point.lon() - 10) / LON_PER_METER;
	}

	private static double north(TrackPoint point)
	{
		return (point.lat() - 50) / LAT_PER_METER;
	}

	@Test
	void shouldMergeTwoDrivesOfAStreet()
	{
		List<TrackPoint> first = northSouth(0, 1000, 0);
		List<TrackPoint> second = northSouth(0, 1000, 6);
		SharedLine shared = shared(List.of(first, second));

		for (TrackPoint point : shared.apply(first))
		{
			assertEquals(3, east(point), 0.5);
		}
		for (TrackPoint point : shared.apply(second))
		{
			assertEquals(3, east(point), 0.5);
		}
	}

	@Test
	void shouldMergeBothDirections()
	{
		List<TrackPoint> there = northSouth(0, 1000, 0);
		List<TrackPoint> back = northSouth(1000, 0, 4);
		SharedLine shared = shared(List.of(there, back));

		for (TrackPoint point : shared.apply(back))
		{
			assertEquals(2, east(point), 0.5);
		}
	}

	@Test
	void shouldMoveThePointsOnlySideways()
	{
		List<TrackPoint> first = northSouth(0, 1000, 0);
		List<TrackPoint> second = northSouth(0, 1000, 6);
		List<TrackPoint> moved = shared(List.of(first, second)).apply(first);

		for (int i = 0; i < first.size(); i++)
		{
			assertEquals(north(first.get(i)), north(moved.get(i)), 0.01);
			assertEquals(first.get(i).ts(), moved.get(i).ts());
			assertEquals(first.get(i).speed(), moved.get(i).speed());
		}
	}

	@Test
	void shouldKeepAStreetDrivenOnce()
	{
		List<TrackPoint> drive = northSouth(0, 1000, 0);

		assertEquals(drive, shared(List.of(drive)).apply(drive));
	}

	@Test
	void shouldNotBePulledByACrossingStreet()
	{
		List<TrackPoint> first = northSouth(0, 1000, 0);
		List<TrackPoint> second = northSouth(0, 1000, 6);
		// driven often, the crossing at 500 m north
		List<List<TrackPoint>> drives = new ArrayList<>(List.of(first, second));
		for (int i = 0; i < 5; i++)
		{
			drives.add(eastWest(500 + i, -300, 300));
		}

		for (TrackPoint point : shared(drives).apply(first))
		{
			assertEquals(3, east(point), 0.5);
		}
	}

	@Test
	void shouldKeepParallelStreetsApart()
	{
		List<TrackPoint> west = northSouth(0, 1000, 0);
		List<TrackPoint> east = northSouth(0, 1000, 40);
		List<TrackPoint> eastAgain = northSouth(0, 1000, 44);
		SharedLine shared = shared(List.of(west, east, eastAgain));

		assertEquals(west, shared.apply(west));
		for (TrackPoint point : shared.apply(east))
		{
			assertEquals(42, east(point), 0.5);
		}
	}

	@Test
	void shouldKeepAPointWithoutADirection()
	{
		// standing: the points don't move
		List<TrackPoint> standing = List.of(point(0, 0), point(0, 0), point(0, 0));
		List<TrackPoint> first = northSouth(-500, 500, 0);
		List<TrackPoint> second = northSouth(-500, 500, 6);
		SharedLine shared = shared(List.of(first, second, standing));

		assertEquals(standing, shared.apply(standing));
	}

	@Test
	void shouldShareAnAxisForBothDirections()
	{
		assertEquals(SharedLine.axis(1, 0), SharedLine.axis(-1, 0));
		assertEquals(SharedLine.axis(0, 1), SharedLine.axis(0, -1));
		assertEquals(SharedLine.axis(1, 1), SharedLine.axis(-1, -1));
		assertEquals(SharedLine.AXES / 2, SharedLine.axis(0, 1));
	}
}
