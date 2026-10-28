package de.codeflowwizardry.carledger.rest.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class TrackPointInputTest
{
	private static final OffsetDateTime TS = OffsetDateTime.parse("2026-10-20T08:00:00Z");

	private static TrackPointInput point(Double accel, Double brake, Double lateral, String gsource)
	{
		return new TrackPointInput(TS, 50.0, 10.0, 42.0, null, null, accel, brake, lateral, gsource).plausible();
	}

	@Test
	void shouldKeepPlausibleGForces()
	{
		TrackPointInput point = point(0.0, 2.0, -2.0, "IMU ");

		assertEquals(0.0, point.accel());
		assertEquals(2.0, point.brake());
		assertEquals(-2.0, point.lateral());
		assertEquals(TrackPointInput.IMU, point.gsource());
	}

	@Test
	void shouldLeaveOutOnlyTheImplausibleOnes()
	{
		TrackPointInput point = point(-0.01, 2.01, 1.2, "gps");

		assertNull(point.accel());
		assertNull(point.brake());
		assertEquals(1.2, point.lateral());
		assertEquals(TrackPointInput.GPS, point.gsource());
		assertEquals(42.0, point.speed());
		assertEquals(TS, point.ts());
	}

	@Test
	void shouldNameTheSource()
	{
		// a firmware before the IMU sends none
		assertEquals(TrackPointInput.GPS, point(0.3, null, null, null).gsource());
		assertEquals(TrackPointInput.GPS, point(0.3, null, null, " ").gsource());
		assertEquals(TrackPointInput.UNKNOWN, point(0.3, null, null, "laser").gsource());
		// nothing to describe
		assertNull(point(null, null, null, "imu").gsource());
		assertNull(point(-1.0, 3.0, 2.5, "imu").gsource());
	}
}
