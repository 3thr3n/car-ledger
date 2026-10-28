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
class PassCounterTest
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

	private TrackFrequency frequency()
	{
		SharedLine shared = new SharedLine(Duration.ofMinutes(5));
		PassCounter counter = new PassCounter(Duration.ofMinutes(5), 5, shared);
		points.forEach(point -> {
			counter.count(point);
			shared.add(point);
		});
		points.forEach(counter::trace);
		return counter.frequency();
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
		assertEquals(6000, length(frequency, 2), 50);
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
		// of both drives
		assertEquals(3000, length(frequency, 2), 50);
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
		assertEquals(6000, length(frequency, 1), 50);
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
		assertTrue(length(frequency, 2) > 5800, () -> "passed twice " + length(frequency, 2));
	}

	@Test
	void shouldBeEmptyWithoutPoints()
	{
		TrackFrequency frequency = frequency();

		assertEquals(0, frequency.maxPasses());
		assertTrue(frequency.lines().isEmpty());
	}
}
