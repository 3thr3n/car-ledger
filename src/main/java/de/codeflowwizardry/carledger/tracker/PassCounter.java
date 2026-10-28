package de.codeflowwizardry.carledger.tracker;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.codeflowwizardry.carledger.rest.car.track.FrequencyLine;
import de.codeflowwizardry.carledger.rest.car.track.TrackFrequency;
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

/**
 * Counts how often the car passed the roads of its points, in two runs over the
 * points in the order of time: {@link #count} all of them, then {@link #trace}
 * all of them again for the lines.
 * <p>
 * The points are laid on a grid of {@link #CELL} meters. A point marks its cell
 * and the eight around it, so two drives on the same road meet in a cell even
 * with the drift of the GPS between them; the pieces between the points are
 * sampled, so a fast car marks every cell it crosses. A cell counts a pass when
 * the car wasn't in it for longer than the gap: standing in it or a jam doesn't
 * add one, the way back after a stop does. The lines are drawn on the
 * {@link SharedLine}, the passes are counted on the raw points.
 */
final class PassCounter
{
	/** meters, the side of a cell */
	static final double CELL = 15;
	/** meters between the samples of a piece */
	private static final double STEP = CELL / 3;
	/** points on each side whose median are the passes of a point */
	private static final int SMOOTHING = 2;

	private static final class Cell
	{
		int passes;
		OffsetDateTime last;
	}

	private final Duration gap;
	private final double tolerance;
	private final SharedLine shared;
	private final Map<Long, Cell> cells = new HashMap<>();
	private final List<FrequencyLine> lines = new ArrayList<>();
	/** of the grid, at the latitude of the first point */
	private double cosLat;
	private TrackPoint previous;
	/** raw points of the segment being traced */
	private List<TrackPoint> segment = new ArrayList<>();

	/**
	 * @param gap
	 *            a longer time without a point starts a new segment
	 * @param tolerance
	 *            meters, of the simplification of the lines
	 * @param shared
	 *            filled in the first run as well, the lines are moved onto it
	 */
	PassCounter(Duration gap, double tolerance, SharedLine shared)
	{
		this.gap = gap;
		this.tolerance = tolerance;
		this.shared = shared;
	}

	/** first run: marks the cells of the point and of the piece to it */
	void count(TrackPoint point)
	{
		if (previous == null)
		{
			cosLat = Math.cos(Math.toRadians(point.lat()));
		}
		if (previous == null || longerThanGap(previous.ts(), point.ts()))
		{
			mark(point.lat(), point.lon(), point.ts());
		} else
		{
			int steps = Math.max(1, (int) Math.ceil(TrackSimplifier.distance(previous, point) / STEP));
			for (int i = 1; i <= steps; i++)
			{
				double f = (double) i / steps;
				mark(previous.lat() + (point.lat() - previous.lat()) * f,
						previous.lon() + (point.lon() - previous.lon()) * f, point.ts());
			}
		}
		previous = point;
	}

	/** second run: cuts the segments into lines of the same passes */
	void trace(TrackPoint point)
	{
		if (!segment.isEmpty() && longerThanGap(segment.getLast().ts(), point.ts()))
		{
			finishSegment();
		}
		segment.add(point);
	}

	/**
	 * @return after both runs
	 */
	TrackFrequency frequency()
	{
		finishSegment();
		lines.sort(Comparator.comparingInt(FrequencyLine::passes));
		return new TrackFrequency(lines, lines.isEmpty() ? 0 : lines.getLast().passes());
	}

	private boolean longerThanGap(OffsetDateTime earlier, OffsetDateTime later)
	{
		return Duration.between(earlier, later).compareTo(gap) > 0;
	}

	private void mark(double lat, double lon, OffsetDateTime ts)
	{
		long x = column(lon);
		long y = row(lat);
		for (long dx = -1; dx <= 1; dx++)
		{
			for (long dy = -1; dy <= 1; dy++)
			{
				Cell cell = cells.computeIfAbsent(key(x + dx, y + dy), key -> new Cell());
				if (cell.last == null || longerThanGap(cell.last, ts))
				{
					cell.passes++;
				}
				cell.last = ts;
			}
		}
	}

	/**
	 * @return of the cell of the point; <code>null</code> for a point that came
	 *         after the first run
	 */
	private Cell cell(TrackPoint point)
	{
		return cells.get(key(column(point.lon()), row(point.lat())));
	}

	/** flat around the latitude of the first point, like the simplification */
	private long column(double lon)
	{
		return (long) Math.floor(Math.toRadians(lon) * TrackSimplifier.EARTH_RADIUS * cosLat / CELL);
	}

	private static long row(double lat)
	{
		return (long) Math.floor(Math.toRadians(lat) * TrackSimplifier.EARTH_RADIUS / CELL);
	}

	private static long key(long x, long y)
	{
		return x << 32 | (y & 0xffffffffL);
	}

	private void finishSegment()
	{
		int size = segment.size();
		int[] passes = new int[size];
		for (int i = 0; i < size; i++)
		{
			Cell cell = cell(segment.get(i));
			passes[i] = cell == null ? 1 : cell.passes;
		}
		// a point at the edge of the cells of another drive doesn't cut the line
		int[] smoothed = new int[size];
		for (int i = 0; i < size; i++)
		{
			int[] window = Arrays.copyOfRange(passes, Math.max(0, i - SMOOTHING), Math.min(size, i + SMOOTHING + 1));
			Arrays.sort(window);
			smoothed[i] = window[window.length / 2];
		}

		List<TrackPoint> moved = shared.apply(segment);
		int start = 0;
		for (int i = 1; i <= size; i++)
		{
			if (i == size || smoothed[i] != smoothed[start])
			{
				// up to the first point of the next line, so they connect
				int end = Math.min(i, size - 1);
				if (end > start)
				{
					addLine(segment.subList(start, end + 1), moved.subList(start, end + 1), smoothed[start]);
				}
				start = i;
			}
		}
		segment = new ArrayList<>();
	}

	/**
	 * @param points
	 *            raw, for the cells
	 * @param moved
	 *            the same on the shared line, drawn
	 */
	private void addLine(List<TrackPoint> points, List<TrackPoint> moved, int passes)
	{
		OffsetDateTime last = points.getLast().ts();
		for (TrackPoint point : points)
		{
			Cell cell = cell(point);
			if (cell != null && cell.last.isAfter(last))
			{
				last = cell.last;
			}
		}
		// the speed doesn't matter here
		List<TrackPoint> kept = TrackSimplifier.simplify(moved, tolerance, Double.POSITIVE_INFINITY);
		double[][] coordinates = new double[kept.size()][];
		for (int i = 0; i < kept.size(); i++)
		{
			coordinates[i] = new double[] {
					kept.get(i).lon(), kept.get(i).lat()
			};
		}
		lines.add(new FrequencyLine(passes, last, coordinates));
	}
}
