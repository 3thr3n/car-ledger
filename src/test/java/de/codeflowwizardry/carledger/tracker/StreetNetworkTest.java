package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.car.track.FrequencyLine;
import de.codeflowwizardry.carledger.rest.car.track.TrackFrequency;
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

/**
 * Drives of a car with a tracker sending every second, on a road to the north
 */
class StreetNetworkTest
{
	private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-20T08:00:00Z");
	/** degrees of latitude per meter */
	private static final double LAT_PER_METER = 1 / 111_195.0;
	private static final double LON_PER_METER = LAT_PER_METER / Math.cos(Math.toRadians(50));

	private final List<TrackPoint> points = new ArrayList<>();
	/** seconds since {@link #START} */
	private int second;

	private void point(double north, double east)
	{
		points.add(new TrackPoint(START.plusSeconds(second++), 50 + north * LAT_PER_METER, 10 + east * LON_PER_METER,
				null));
	}

	/**
	 * 15 m per second, the drift of the GPS zigzags around the east
	 */
	private void drive(double fromNorth, double toNorth, double east)
	{
		double step = fromNorth < toNorth ? 15 : -15;
		int i = 0;
		for (double north = fromNorth; step > 0 ? north <= toNorth : north >= toNorth; north += step)
		{
			point(north, east + (i++ % 2 == 0 ? 4 : -4));
		}
	}

	private void park(int seconds)
	{
		second += seconds;
	}

	/** the tracker keeps sending, the GPS drifts by a few meters */
	private void stand(double north, int seconds)
	{
		for (int i = 0; i < seconds; i++)
		{
			point(north + i % 3, i % 2 == 0 ? 5 : -5);
		}
	}

	/** the points split at the gaps, as the service does */
	private TrackFrequency frequency()
	{
		List<List<TrackPoint>> segments = new ArrayList<>();
		for (TrackPoint point : points)
		{
			if (segments.isEmpty() || Duration.between(segments.getLast().getLast().ts(), point.ts())
					.compareTo(Duration.ofMinutes(5)) > 0)
			{
				segments.add(new ArrayList<>());
			}
			segments.getLast().add(point);
		}
		return StreetNetwork.frequency(segments, Duration.ofMinutes(5), 1);
	}

	/** meters to the north, of all lines with the passes */
	private static double length(TrackFrequency frequency, int passes)
	{
		return frequency.lines().stream()
				.filter(line -> line.passes() == passes)
				.mapToDouble(line -> {
					double[][] c = line.coordinates();
					return Math.abs(c[c.length - 1][1] - c[0][1]) / LAT_PER_METER;
				})
				.sum();
	}

	@Test
	void shouldCountTheSameRoadTwice()
	{
		drive(0, 3000, 0);
		park(8 * 3600);
		// back home, a few meters next to the way there
		drive(3000, 0, 6);

		TrackFrequency frequency = frequency();

		assertEquals(2, frequency.maxPasses());
		assertEquals(0, length(frequency, 1), 50);
		// the street once, for both drives
		assertEquals(3000, length(frequency, 2), 50);
		assertEquals(1, frequency.lines().size());
		FrequencyLine line = frequency.lines().getLast();
		assertEquals(START.plusSeconds(second - 1), line.last());
	}

	@Test
	void shouldCountOnlyWhereTheDrivesMeet()
	{
		drive(0, 3000, 0);
		park(3600);
		drive(1500, 3000, 0);

		TrackFrequency frequency = frequency();

		assertEquals(2, frequency.maxPasses());
		assertEquals(1500, length(frequency, 1), 50);
		assertEquals(1500, length(frequency, 2), 50);
		// the more frequent last, drawn on top
		assertEquals(2, frequency.lines().getLast().passes());
	}

	@Test
	void shouldNotCountAParallelRoad()
	{
		drive(0, 3000, 0);
		park(3600);
		drive(0, 3000, 80);

		TrackFrequency frequency = frequency();

		assertEquals(1, frequency.maxPasses());
		// two streets
		assertEquals(6000, length(frequency, 1), 50);
		assertEquals(2, frequency.lines().size());
	}

	@Test
	void shouldNotCountAStandingCar()
	{
		drive(0, 1500, 0);
		stand(1500, 1200);
		drive(1500, 3000, 0);

		TrackFrequency frequency = frequency();

		assertEquals(1, frequency.maxPasses());
	}

	@Test
	void shouldCountTheWayBackAfterAStop()
	{
		drive(0, 3000, 0);
		stand(3000, 600);
		drive(3000, 0, 0);

		TrackFrequency frequency = frequency();

		assertEquals(2, frequency.maxPasses());
		// only around the stop the car never left
		assertTrue(length(frequency, 2) > 2900, () -> "passed twice " + length(frequency, 2));
	}

	@Test
	void shouldBeEmptyWithoutPoints()
	{
		TrackFrequency frequency = frequency();

		assertEquals(0, frequency.maxPasses());
		assertTrue(frequency.lines().isEmpty());
	}

	@Test
	void shouldMakeOneStreetOfDrivesAlongTheEdgeOfTheCells()
	{
		// both drives around the edge of two columns of cells (x = 8 m)
		for (int i = 0; i < 3; i++)
		{
			drive(0, 3000, 6);
			park(3600);
			drive(3000, 0, 10);
			park(3600);
		}

		TrackFrequency frequency = frequency();

		assertEquals(1, frequency.lines().size(), () -> "lines " + frequency.lines().size());
		assertEquals(6, frequency.maxPasses());
	}

	@Test
	void shouldSplitTheStreetsAtACrossing()
	{
		drive(0, 3000, 0);
		park(3600);
		// to the east at 1500 m north, across
		for (double east = -1500; east <= 1500; east += 15)
		{
			point(1500, east);
		}

		TrackFrequency frequency = frequency();

		// four arms of the crossing
		assertEquals(4, frequency.lines().size(), () -> "lines " + frequency.lines().size());
		assertEquals(1, frequency.maxPasses());
	}

	@Test
	void shouldKeepASmallRoundabout()
	{
		// around a roundabout of 15 m radius, slowly, a point a second
		for (int i = 0; i <= 47; i++)
		{
			double angle = 2 * Math.PI * i / 47;
			point(15 * Math.sin(angle), 15 * Math.cos(angle));
		}

		TrackFrequency frequency = frequency();

		// all the way around: no detour of the grid to remove
		double around = frequency.lines().stream().mapToDouble(line -> {
			double[][] c = line.coordinates();
			double meters = 0;
			for (int k = 1; k < c.length; k++)
			{
				meters += Math.hypot((c[k][0] - c[k - 1][0]) / LON_PER_METER, (c[k][1] - c[k - 1][1]) / LAT_PER_METER);
			}
			return meters;
		}).sum();
		assertEquals(2 * Math.PI * 15, around, 15);
	}
}
