package de.codeflowwizardry.carledger.tracker;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

import de.codeflowwizardry.carledger.data.repository.TrackPointRepository.OnRoad;
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

/**
 * Puts the vertices of the road between the matched points, so the track
 * follows the curves instead of cutting them, and two drives of a road lie on
 * the same line. A vertex gets the time and the speed between its two points
 * by its distance along the road, and the g-forces of the point after it: they
 * are the peaks of that piece. Only between two points read one after the
 * other, within {@link TrackService#GAP}: the first point of a range has no
 * road.
 */
final class Road implements OnRoad
{
	private final Consumer<TrackPoint> next;
	private TrackPoint previous;

	/**
	 * @param next
	 *            gets the points and the vertices between them, in the order of
	 *            time
	 */
	Road(Consumer<TrackPoint> next)
	{
		this.next = next;
	}

	@Override
	public void accept(TrackPoint point, String road)
	{
		if (road != null && previous != null && !point.ts().isBefore(previous.ts())
				&& Duration.between(previous.ts(), point.ts()).compareTo(TrackService.GAP) <= 0)
		{
			vertices(point, road);
		}
		next.accept(point);
		previous = point;
	}

	private void vertices(TrackPoint point, String road)
	{
		double[] vertices;
		try
		{
			vertices = Polyline.decode(road);
		}
		catch (IllegalArgumentException e)
		{
			// a straight line, as without the road
			return;
		}
		int count = vertices.length / 2;
		TrackPoint[] between = new TrackPoint[count];
		double[] along = new double[count];
		double length = 0;
		TrackPoint last = previous;
		for (int k = 0; k < count; k++)
		{
			between[k] = new TrackPoint(null, vertices[k * 2], vertices[k * 2 + 1], null);
			length += TrackSimplifier.distance(last, between[k]);
			along[k] = length;
			last = between[k];
		}
		length += TrackSimplifier.distance(last, point);

		long nanos = Duration.between(previous.ts(), point.ts()).toNanos();
		for (int k = 0; k < count; k++)
		{
			double share = length == 0 ? 0 : along[k] / length;
			OffsetDateTime ts = previous.ts().plusNanos(Math.round(nanos * share));
			Double speed = previous.speed() == null || point.speed() == null ? point.speed()
					: previous.speed() + (point.speed() - previous.speed()) * share;
			next.accept(new TrackPoint(ts, between[k].lat(), between[k].lon(), speed, point.g()));
		}
	}
}
