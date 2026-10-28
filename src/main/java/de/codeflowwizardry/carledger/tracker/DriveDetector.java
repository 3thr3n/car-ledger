package de.codeflowwizardry.carledger.tracker;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

/**
 * Finds the drives in the points of a car, passed in the order of time. A drive
 * ends with a stop: a gap without points, or a car that doesn't move for longer
 * than the stop, e.g. a tracker that keeps sending while parked. Not moving
 * means staying within {@link #DRIFT} of the point where the car arrived, so
 * the drift of the GPS doesn't start a drive, and a red light or a jam that
 * moves doesn't end one. A drive needs a stop before it as well: one already on
 * the road at the start of the points (e.g. the rest of a trip) isn't one.
 */
final class DriveDetector
{
	/** meters a standing car seems to move, GPS drift */
	static final double DRIFT = 30;

	/**
	 * @param meters
	 *            GPS distance from the start to the end
	 * @param departure
	 *            the point it left from
	 * @param arrival
	 *            the point it arrived at, where the car stopped
	 */
	record Drive(OffsetDateTime start, OffsetDateTime end, double meters, TrackPoint departure, TrackPoint arrival)
	{
	}

	private final Duration stop;
	private final double minMeters;
	private final Duration minDuration;
	private final OffsetDateTime from;
	private final List<Drive> drives = new ArrayList<>();

	/** a stop was seen, a drive from now on started from it */
	private boolean stopped;
	/** of the current drive */
	private boolean startedFromStop;

	private TrackPoint previous;
	/** where the car arrived last, the points since are within the drift */
	private TrackPoint anchor;
	/** of the drive, <code>null</code> while the car stands */
	private TrackPoint start;
	/** of the drive until the anchor */
	private double meters;

	/**
	 * @param from
	 *            the points start at this time; no point for a stop before the
	 *            first one is a stop as well. <code>null</code> to count a drive
	 *            already on the road at the first point, from there.
	 */
	DriveDetector(Duration stop, double minMeters, Duration minDuration, OffsetDateTime from)
	{
		this.stop = stop;
		this.minMeters = minMeters;
		this.minDuration = minDuration;
		this.from = from;
	}

	void add(TrackPoint point)
	{
		if (anchor == null)
		{
			anchor = point;
			stopped = from == null || longerThanStop(from, point.ts());
		} else if (longerThanStop(previous.ts(), point.ts()))
		{
			// the tracker was off, the car parked
			end();
			anchor = point;
		} else
		{
			double moved = TrackSimplifier.distance(anchor, point);
			if (moved > DRIFT)
			{
				if (start == null)
				{
					// left from the last point it stood at
					start = previous;
					startedFromStop = stopped;
					moved = TrackSimplifier.distance(start, point);
				}
				// from anchor to anchor: the drift of a standing car doesn't add up
				meters += moved;
				anchor = point;
			} else if (longerThanStop(anchor.ts(), point.ts()))
			{
				end();
			}
		}
		previous = point;
	}

	/**
	 * @param now
	 *            a drive without a stop until then is still on the road, it isn't
	 *            one yet
	 * @return the drives, without the ones shorter than the minimum distance or
	 *         duration
	 */
	List<Drive> drives(OffsetDateTime now)
	{
		if (start != null && longerThanStop(anchor.ts(), now))
		{
			end();
		}
		return drives;
	}

	private boolean longerThanStop(OffsetDateTime earlier, OffsetDateTime later)
	{
		return Duration.between(earlier, later).compareTo(stop) > 0;
	}

	/** the car stops, a drive ends where it arrived */
	private void end()
	{
		if (start != null && startedFromStop)
		{
			Drive drive = new Drive(start.ts(), anchor.ts(), meters, start, anchor);
			if (drive.meters() >= minMeters
					&& Duration.between(drive.start(), drive.end()).compareTo(minDuration) >= 0)
			{
				drives.add(drive);
			}
		}
		start = null;
		meters = 0;
		stopped = true;
	}
}
