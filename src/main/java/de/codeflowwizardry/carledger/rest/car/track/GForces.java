package de.codeflowwizardry.carledger.rest.car.track;

/**
 * The g-forces of a track point: the peaks since the point before, in g. A
 * value the tracker didn't send, or an implausible one, is missing.
 *
 * @param accel
 *            peak forward acceleration, 0 or more
 * @param brake
 *            peak braking, 0 or more
 * @param lateral
 *            peak sideways acceleration, + turning right, - turning left
 * @param imu
 *            real peaks of the IMU; else averages over a second of the GPS
 *            (also of an unknown source), noticeably lower
 */
public record GForces(Double accel, Double brake, Double lateral, boolean imu)
{
	/**
	 * @return <code>null</code> without any value
	 */
	public static GForces of(Double accel, Double brake, Double lateral, String source)
	{
		if (accel == null && brake == null && lateral == null)
		{
			return null;
		}
		return new GForces(accel, brake, lateral, "imu".equals(source));
	}

	/**
	 * The peaks of both, e.g. of a point left out by the simplification and the
	 * next one: the line to the next one still shows them. IMU, if one of them
	 * is.
	 */
	public static GForces merge(GForces a, GForces b)
	{
		if (a == null)
		{
			return b;
		}
		if (b == null)
		{
			return a;
		}
		return new GForces(max(a.accel, b.accel), max(a.brake, b.brake), stronger(a.lateral, b.lateral),
				a.imu || b.imu);
	}

	private static Double max(Double a, Double b)
	{
		return a == null ? b : b == null ? a : Math.max(a, b);
	}

	/** the one further from 0, with its side */
	private static Double stronger(Double a, Double b)
	{
		return a == null ? b : b == null ? a : Math.abs(b) > Math.abs(a) ? b : a;
	}
}
