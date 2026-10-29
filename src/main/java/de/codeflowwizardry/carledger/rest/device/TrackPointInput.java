package de.codeflowwizardry.carledger.rest.device;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * A point of a GPS tracker
 *
 * @param ts
 *            GPS time, ISO-8601 UTC
 * @param speed
 *            km/h
 * @param alt
 *            meters
 * @param accel
 *            peak forward acceleration since the point before, g, 0 or more
 * @param brake
 *            peak braking since the point before, g, 0 or more
 * @param lateral
 *            peak sideways acceleration since the point before, g, + turning
 *            right, - turning left
 * @param gsource
 *            where the g-forces come from: imu, gps or unknown
 */
public record TrackPointInput(OffsetDateTime ts, Double lat, Double lon, Double speed, Double alt, Double hdop,
		Double accel, Double brake, Double lateral, String gsource)
{
	public static final double MAX_SPEED = 400;
	/** g, more isn't plausible even for a performance car */
	public static final double MAX_G = 2.0;

	public static final String IMU = "imu";
	public static final String GPS = "gps";
	public static final String UNKNOWN = "unknown";

	/** GPS trackers didn't send to CarLedger before */
	static final OffsetDateTime EARLIEST = OffsetDateTime.parse("2020-01-01T00:00:00Z");
	/** clock of the tracker ahead of the server */
	static final long MAX_FUTURE_MINUTES = 5;

	/** a point without g-forces, e.g. of a firmware before them */
	public TrackPointInput(OffsetDateTime ts, Double lat, Double lon, Double speed, Double alt, Double hdop)
	{
		this(ts, lat, lon, speed, alt, hdop, null, null, null, null);
	}

	/**
	 * @return whether the point can be stored: a time not in the future and not
	 *         before 2020, a position on earth, a speed of 0 - 400 km/h and a
	 *         positive hdop. Implausible g-forces don't count, see
	 *         {@link #plausible()}.
	 */
	public boolean isValid(OffsetDateTime now)
	{
		return ts != null && !ts.isBefore(EARLIEST) && !ts.isAfter(now.plusMinutes(MAX_FUTURE_MINUTES))
				&& lat != null && lat >= -90 && lat <= 90
				&& lon != null && lon >= -180 && lon <= 180
				&& (speed == null || speed >= 0 && speed <= MAX_SPEED)
				&& (hdop == null || hdop > 0);
	}

	/**
	 * @return the point without the implausible g-forces: accel and brake of 0 to
	 *         {@link #MAX_G}, lateral within ±{@link #MAX_G}; the source as
	 *         {@link #IMU}, {@link #GPS} or {@link #UNKNOWN} for any other, and
	 *         {@link #GPS} when it is missing next to values (firmware before the
	 *         IMU); none without values
	 */
	public TrackPointInput plausible()
	{
		Double a = accel != null && accel >= 0 && accel <= MAX_G ? accel : null;
		Double b = brake != null && brake >= 0 && brake <= MAX_G ? brake : null;
		Double l = lateral != null && Math.abs(lateral) <= MAX_G ? lateral : null;
		String source = null;
		if (a != null || b != null || l != null)
		{
			String sent = gsource == null ? "" : gsource.strip().toLowerCase(Locale.ROOT);
			source = sent.isEmpty() ? GPS : sent.equals(IMU) || sent.equals(GPS) ? sent : UNKNOWN;
		}
		return new TrackPointInput(ts, lat, lon, speed, alt, hdop, a, b, l, source);
	}
}
