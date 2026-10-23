package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;
import de.codeflowwizardry.carledger.tracker.DriveDetector.Drive;

/**
 * A car with a tracker sending every second: it drives north, stands with the
 * drift of the GPS or has no points while the tracker is off
 */
class DriveDetectorTest
{
	private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-20T08:00:00Z");
	/** degrees of latitude per meter */
	private static final double LAT_PER_METER = 1 / 111_195.0;

	private DriveDetector detector = detector(START.minusHours(1));

	private static DriveDetector detector(OffsetDateTime from)
	{
		return new DriveDetector(Duration.ofMinutes(5), 500, Duration.ofMinutes(2), from);
	}

	/** seconds since {@link #START} */
	private int second;
	/** meters north */
	private double north;

	private void point(double driftEast)
	{
		detector.add(new TrackPoint(START.plusSeconds(second), 50 + north * LAT_PER_METER,
				10 + driftEast * LAT_PER_METER / Math.cos(Math.toRadians(50)), null));
		second++;
	}

	/** meters per second, for the seconds */
	private void drive(double speed, int seconds)
	{
		for (int i = 0; i < seconds; i++)
		{
			north += speed;
			point(0);
		}
	}

	/** stands still, the GPS drifts by up to 12 m */
	private void stand(int seconds)
	{
		for (int i = 0; i < seconds; i++)
		{
			point(i % 3 == 0 ? 12 : i % 3 == 1 ? -8 : 0);
		}
	}

	/** the tracker is off */
	private void off(int seconds)
	{
		second += seconds;
	}

	private OffsetDateTime at(int second)
	{
		return START.plusSeconds(second);
	}

	/**
	 * The drift radius delays the start and brings the end forward by a few
	 * seconds, nothing that matters for a trip
	 */
	private void assertAbout(int second, OffsetDateTime actual)
	{
		assertTrue(Math.abs(Duration.between(at(second), actual).toSeconds()) <= 3,
				() -> "expected about " + at(second) + " but was " + actual);
	}

	private List<Drive> drives()
	{
		return detector.drives(at(second + 3600));
	}

	@Test
	void shouldEndADriveWhenTheParkedCarKeepsSending()
	{
		stand(60);
		drive(14, 600);
		stand(1200);
		drive(14, 300);
		stand(600);

		List<Drive> drives = drives();

		assertEquals(2, drives.size());
		// left from the last point it stood at, ends where it arrived
		assertAbout(59, drives.get(0).start());
		assertAbout(660, drives.get(0).end());
		assertEquals(8400, drives.get(0).meters(), 50);
		assertAbout(1859, drives.get(1).start());
		assertAbout(2160, drives.get(1).end());
		assertEquals(4200, drives.get(1).meters(), 50);
	}

	@Test
	void shouldNotEndADriveAtARedLightOrInAJam()
	{
		drive(14, 300);
		stand(120);
		// stop and go, 1 m/s for 10 minutes
		drive(1, 600);
		stand(240);
		drive(14, 300);
		stand(600);

		List<Drive> drives = drives();

		assertEquals(1, drives.size());
		assertAbout(0, drives.get(0).start());
		assertAbout(1560, drives.get(0).end());
		assertEquals(14 * 600 + 600, drives.get(0).meters(), 100);
	}

	@Test
	void shouldEndADriveAtAGap()
	{
		drive(14, 300);
		off(600);
		drive(14, 300);
		off(600);

		List<Drive> drives = drives();

		assertEquals(2, drives.size());
		assertAbout(299, drives.get(0).end());
		assertAbout(900, drives.get(1).start());
	}

	@Test
	void shouldIgnoreShortDrivesAndTheDrift()
	{
		stand(600);
		// parking: 300 m
		drive(5, 60);
		stand(600);
		// far, but under 2 minutes
		drive(30, 100);
		stand(600);

		assertTrue(drives().isEmpty());
	}

	@Test
	void shouldNotSuggestADriveStillOnTheRoad()
	{
		drive(14, 300);
		stand(120);

		// 2 minutes after the last point the car may drive on
		assertTrue(detector.drives(at(second + 60)).isEmpty());
		assertEquals(1, new DriveDetectorTest().driveThenAsk(6 * 60).size());
	}

	/** a drive of 5 minutes, then the question that much later */
	private List<Drive> driveThenAsk(int secondsLater)
	{
		drive(14, 300);
		return detector.drives(at(second + secondsLater));
	}

	@Test
	void shouldNotCountADriveOnTheRoadAtTheStart()
	{
		// e.g. the rest of a trip that ended a moment before
		detector = detector(START);
		drive(14, 600);
		stand(600);
		drive(14, 300);
		stand(600);

		List<Drive> drives = drives();

		assertEquals(1, drives.size());
		assertAbout(1199, drives.get(0).start());
	}

	@Test
	void shouldCountADriveAfterAQuietStart()
	{
		detector = detector(START.minusMinutes(10));
		drive(14, 600);
		stand(600);

		assertEquals(1, drives().size());
	}
}
