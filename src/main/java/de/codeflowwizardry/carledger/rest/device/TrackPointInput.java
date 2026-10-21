package de.codeflowwizardry.carledger.rest.device;

import java.time.OffsetDateTime;

/**
 * A point of a GPS tracker
 *
 * @param ts
 *            GPS time, ISO-8601 UTC
 * @param speed
 *            km/h
 * @param alt
 *            meters
 */
public record TrackPointInput(OffsetDateTime ts, Double lat, Double lon, Double speed, Double alt, Double hdop)
{
	public static final double MAX_SPEED = 400;

	/** GPS trackers didn't send to CarLedger before */
	static final OffsetDateTime EARLIEST = OffsetDateTime.parse("2020-01-01T00:00:00Z");
	/** clock of the tracker ahead of the server */
	static final long MAX_FUTURE_MINUTES = 5;

	/**
	 * @return whether the point can be stored: a time not in the future and not
	 *         before 2020, a position on earth, a speed of 0 - 400 km/h and a
	 *         positive hdop
	 */
	public boolean isValid(OffsetDateTime now)
	{
		return ts != null && !ts.isBefore(EARLIEST) && !ts.isAfter(now.plusMinutes(MAX_FUTURE_MINUTES))
				&& lat != null && lat >= -90 && lat <= 90
				&& lon != null && lon >= -180 && lon <= 180
				&& (speed == null || speed >= 0 && speed <= MAX_SPEED)
				&& (hdop == null || hdop > 0);
	}
}
