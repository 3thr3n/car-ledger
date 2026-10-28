package de.codeflowwizardry.carledger.tracker;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

/**
 * One line per street for the map: the GPS drifts by a few meters from drive to
 * drive and the two directions use different lanes, so a street driven several
 * times would show as lines next to each other. Two runs over the points in the
 * order of time: {@link #add} all of them, then {@link #apply} to each segment.
 * <p>
 * The first run samples the pieces between the points every few meters, so a
 * fast and a slow drive weigh the same per meter, and sums the samples on a
 * grid
 * of {@link #CELL} meters, per axis of the direction (both directions of a
 * street share one). The second run moves each point sideways, never along the
 * street, onto the average of the samples around it with about its direction: a
 * crossing street doesn't pull it. A street passed only once stays as it is.
 * Only for display: the raw points aren't changed.
 */
final class SharedLine
{
	/** meters, the side of a cell; the average covers the eight around it too */
	static final double CELL = 8;
	/** meters between the samples of a piece */
	private static final double STEP = CELL / 2;
	/** a shorter piece has no direction, e.g. a car standing */
	private static final double MIN_PIECE = 1;
	/**
	 * axes of the direction over 180°, a sample counts for its axis and the two
	 * next to it
	 */
	static final int AXES = 8;

	/** the sum of the samples of a cell and axis */
	private static final class Cell
	{
		double x;
		double y;
		int samples;
	}

	/** the passes through a cell and the eight around it, in any direction */
	private static final class Passes
	{
		int count;
		OffsetDateTime last;
	}

	private final Duration gap;
	private final Map<Long, Cell> cells = new HashMap<>();
	private final Map<Long, Passes> passes = new HashMap<>();
	/** of the flat projection, at the first point */
	private double lat0;
	private double lon0;
	private double cosLat;
	private TrackPoint previous;

	/**
	 * @param gap
	 *            a longer time without a point starts a new segment
	 */
	SharedLine(Duration gap)
	{
		this.gap = gap;
	}

	/** first run: sums the samples of the piece to the point */
	void add(TrackPoint point)
	{
		if (previous == null)
		{
			lat0 = point.lat();
			lon0 = point.lon();
			cosLat = Math.cos(Math.toRadians(lat0));
		}
		if (previous != null && !longerThanGap(previous.ts(), point.ts()))
		{
			double[] a = project(previous);
			double[] b = project(point);
			double length = Math.hypot(b[0] - a[0], b[1] - a[1]);
			if (length >= MIN_PIECE)
			{
				int axis = axis(b[0] - a[0], b[1] - a[1]);
				int steps = (int) Math.ceil(length / STEP);
				for (int i = 1; i <= steps; i++)
				{
					double f = (double) i / steps;
					sample(a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, axis, point.ts());
				}
			}
		}
		previous = point;
	}

	/**
	 * Second run, after all points were added
	 *
	 * @param segment
	 *            points without a gap between them
	 * @return the points moved onto the shared line, in their order
	 */
	List<TrackPoint> apply(List<TrackPoint> segment)
	{
		if (cells.isEmpty())
		{
			return segment;
		}
		int size = segment.size();
		double[][] projected = new double[size][];
		for (int i = 0; i < size; i++)
		{
			projected[i] = project(segment.get(i));
		}

		List<TrackPoint> moved = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
		{
			moved.add(move(segment.get(i), projected, i));
		}
		return moved;
	}

	private TrackPoint move(TrackPoint point, double[][] projected, int i)
	{
		// the direction at the point: from the one before to the one after
		double[] before = projected[Math.max(0, i - 1)];
		double[] after = projected[Math.min(projected.length - 1, i + 1)];
		double dx = after[0] - before[0];
		double dy = after[1] - before[1];
		double length = Math.hypot(dx, dy);
		if (length < MIN_PIECE)
		{
			return point;
		}

		double px = projected[i][0];
		double py = projected[i][1];
		int axis = axis(dx, dy);
		long column = cell(px);
		long row = cell(py);
		Passes own = passes.get(key(column, row, 0));
		if (own == null || own.count < 2)
		{
			return point;
		}

		double x = 0;
		double y = 0;
		int samples = 0;
		for (int a = axis - 1; a <= axis + 1; a++)
		{
			int near = Math.floorMod(a, AXES);
			for (long c = column - 1; c <= column + 1; c++)
			{
				for (long r = row - 1; r <= row + 1; r++)
				{
					Cell cell = cells.get(key(c, r, near));
					if (cell != null)
					{
						x += cell.x;
						y += cell.y;
						samples += cell.samples;
					}
				}
			}
		}
		if (samples == 0)
		{
			return point;
		}

		// only the part across the direction
		double ux = dx / length;
		double uy = dy / length;
		double ox = x / samples - px;
		double oy = y / samples - py;
		double along = ox * ux + oy * uy;
		double mx = px + ox - along * ux;
		double my = py + oy - along * uy;
		return new TrackPoint(point.ts(), Math.toDegrees(my / TrackSimplifier.EARTH_RADIUS) + lat0,
				Math.toDegrees(mx / (TrackSimplifier.EARTH_RADIUS * cosLat)) + lon0, point.speed());
	}

	private void sample(double x, double y, int axis, OffsetDateTime ts)
	{
		long column = cell(x);
		long row = cell(y);
		Cell cell = cells.computeIfAbsent(key(column, row, axis), key -> new Cell());
		cell.x += x;
		cell.y += y;
		cell.samples++;
		// a drive next to it in the cell around counts as well
		for (long c = column - 1; c <= column + 1; c++)
		{
			for (long r = row - 1; r <= row + 1; r++)
			{
				Passes pass = passes.computeIfAbsent(key(c, r, 0), key -> new Passes());
				if (pass.last == null || longerThanGap(pass.last, ts))
				{
					pass.count++;
				}
				pass.last = ts;
			}
		}
	}

	private boolean longerThanGap(OffsetDateTime earlier, OffsetDateTime later)
	{
		return Duration.between(earlier, later).compareTo(gap) > 0;
	}

	/** meters east and north of the first point, flat like the simplification */
	private double[] project(TrackPoint point)
	{
		return new double[] {
				Math.toRadians(point.lon() - lon0) * TrackSimplifier.EARTH_RADIUS * cosLat,
				Math.toRadians(point.lat() - lat0) * TrackSimplifier.EARTH_RADIUS
		};
	}

	/** the axis of the direction, both directions of a street share it */
	static int axis(double dx, double dy)
	{
		double angle = Math.atan2(dy, dx);
		if (angle < 0)
		{
			angle += Math.PI;
		}
		return Math.floorMod((int) Math.floor(angle / Math.PI * AXES), AXES);
	}

	private static long cell(double meters)
	{
		return (long) Math.floor(meters / CELL);
	}

	private static long key(long column, long row, int axis)
	{
		return (column & 0xFFFFFFFL) << 31 | (row & 0xFFFFFFFL) << 3 | axis;
	}
}
