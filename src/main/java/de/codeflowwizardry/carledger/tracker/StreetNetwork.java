package de.codeflowwizardry.carledger.tracker;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.codeflowwizardry.carledger.rest.car.track.FrequencyLine;
import de.codeflowwizardry.carledger.rest.car.track.TrackFrequency;
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;
import de.codeflowwizardry.carledger.rest.car.track.TrackSegment;

/**
 * The streets the car drove, for the frequency: every street once, whatever
 * drives used it, so they share exactly the same line.
 * <p>
 * The drives, moved onto their {@link SharedLine} first, are sampled every few
 * meters on a grid of {@link #CELL} meters. A cell keeps the average of its
 * samples, its position on the map, and how often the car passed it: like the
 * passes before, a sample marks its cell and the eight around, and a cell
 * counts again once the car was away from it for longer than the gap. Then each
 * drive is a path from cell to cell; where a street runs along the edge of the
 * cells, a sample goes to the cell across the street with more samples, so two
 * drives don't make a ladder of two paths. The steps of all paths are the
 * network; its pieces between junctions and ends are the streets, each with the
 * passes of most of its cells.
 */
final class StreetNetwork
{
	/** meters, the side of a cell, as of the shared line */
	static final double CELL = SharedLine.CELL;
	/** meters between the samples of a piece */
	private static final double STEP = CELL / 2;
	/** a shorter piece has no direction, e.g. a car standing */
	private static final double MIN_PIECE = 1;
	/** cells of a dead end that are noise of the grid at a street */
	static final int SPUR = 2;
	/**
	 * steps of a detour that make a step of fewer drives noise of the grid (40 m):
	 * the drives of a street go from cell to cell a little differently, also around
	 * a crossing; real loops are longer, a small roundabout has 60 to 100 m
	 */
	static final int DETOUR = 5;
	/**
	 * axes on each side a pass counts for: ±56°, the drives of a curve differ in
	 * their direction, a crossing street is 90° off
	 */
	static final int NEAR_AXES = 2;
	/** cells on each side whose median are the passes of a cell of a street */
	private static final int SMOOTHING = 2;

	private static final class Cell
	{
		double x;
		double y;
		int samples;
		/**
		 * by the axis of the direction: a crossing street doesn't add to them, both
		 * directions of a street do
		 */
		final int[] passes = new int[SharedLine.AXES];
		/** epoch millis the car was last here, in whatever direction, 0 never */
		long last;
	}

	private final Duration gap;
	private final long gapMillis;
	private final Map<Long, Cell> cells = new HashMap<>();
	/**
	 * the steps of the paths, the smaller key first, and how many drives took them
	 */
	private final Map<List<Long>, Integer> steps = new HashMap<>();
	/** of the flat projection, at the first point */
	private double lat0;
	private double lon0;
	private double cosLat;

	private StreetNetwork(Duration gap)
	{
		this.gap = gap;
		this.gapMillis = gap.toMillis();
	}

	/**
	 * @param segments
	 *            the drives without a gap, in the order of time
	 * @param tolerance
	 *            meters, of the simplification of the streets
	 */
	static TrackFrequency frequency(List<List<TrackPoint>> segments, Duration gap, double tolerance)
	{
		SharedLine shared = new SharedLine(gap);
		for (List<TrackPoint> segment : segments)
		{
			segment.forEach(shared::add);
			shared.cut();
		}
		List<List<TrackPoint>> moved = segments.stream().map(shared::apply).toList();

		StreetNetwork network = new StreetNetwork(gap);
		moved.forEach(network::count);
		moved.forEach(network::trace);
		List<FrequencyLine> lines = network.streets(tolerance);
		lines.sort(Comparator.comparingInt(FrequencyLine::passes));
		return new TrackFrequency(lines, lines.isEmpty() ? 0 : lines.getLast().passes());
	}

	/** first run: the samples and the passes of the cells */
	private void count(List<TrackPoint> segment)
	{
		long[] previous = {
				Long.MIN_VALUE, -1
		};
		sample(segment, (x, y, dx, dy, ts) -> {
			long column = cell(x);
			long row = cell(y);
			Cell cell = cells.computeIfAbsent(key(column, row), key -> new Cell());
			cell.x += x;
			cell.y += y;
			cell.samples++;
			int direction = SharedLine.axis(dx, dy);
			// the same cell in the same direction marks the same cells again
			if (previous[0] == key(column, row) && previous[1] == direction)
			{
				return;
			}
			previous[0] = key(column, row);
			previous[1] = direction;
			long millis = ts.toInstant().toEpochMilli();
			for (long c = column - 1; c <= column + 1; c++)
			{
				for (long r = row - 1; r <= row + 1; r++)
				{
					Cell near = cells.computeIfAbsent(key(c, r), key -> new Cell());
					// standing or a jam doesn't add a pass when the car drives on, in whatever
					// direction it stood
					boolean away = near.last == 0 || millis - near.last > gapMillis;
					// for the axes near it as well: drives of a curve fall on either side of the
					// edge of two axes
					for (int a = direction - NEAR_AXES; a <= direction + NEAR_AXES; a++)
					{
						int axis = Math.floorMod(a, SharedLine.AXES);
						if (away || near.passes[axis] == 0)
						{
							near.passes[axis]++;
						}
					}
					near.last = millis;
				}
			}
		});
	}

	/** second run: the path of the drive from cell to cell */
	private void trace(List<TrackPoint> segment)
	{
		long[] previous = {
				Long.MIN_VALUE
		};
		// each drive counts once per step
		Set<List<Long>> taken = new HashSet<>();
		sample(segment, (x, y, dx, dy, ts) -> {
			double length = Math.hypot(dx, dy);
			// across the street
			double nx = -dy / length * CELL;
			double ny = dx / length * CELL;
			long cell = key(cell(x), cell(y));
			for (long other : new long[] {
					key(cell(x + nx), cell(y + ny)), key(cell(x - nx), cell(y - ny))
			})
			{
				if (samples(other) > samples(cell))
				{
					cell = other;
				}
			}
			if (previous[0] != Long.MIN_VALUE && previous[0] != cell)
			{
				List<Long> step = step(previous[0], cell);
				if (taken.add(step))
				{
					steps.merge(step, 1, Integer::sum);
				}
			}
			previous[0] = cell;
		});
	}

	private int samples(long key)
	{
		Cell cell = cells.get(key);
		return cell == null ? 0 : cell.samples;
	}

	private interface Sample
	{
		void at(double x, double y, double dx, double dy, OffsetDateTime ts);
	}

	/** every few meters along the pieces between the points */
	private void sample(List<TrackPoint> segment, Sample sample)
	{
		for (int i = 1; i < segment.size(); i++)
		{
			TrackPoint from = segment.get(i - 1);
			TrackPoint to = segment.get(i);
			double[] a = project(from);
			double[] b = project(to);
			double dx = b[0] - a[0];
			double dy = b[1] - a[1];
			double length = Math.hypot(dx, dy);
			if (length < MIN_PIECE)
			{
				continue;
			}
			int count = (int) Math.ceil(length / STEP);
			for (int k = i == 1 ? 0 : 1; k <= count; k++)
			{
				double f = (double) k / count;
				sample.at(a[0] + dx * f, a[1] + dy * f, dx, dy, to.ts());
			}
		}
	}

	/** the pieces of the network between junctions and ends, simplified */
	private List<FrequencyLine> streets(double tolerance)
	{
		Map<Long, List<Long>> next = new LinkedHashMap<>();
		for (List<Long> step : steps.keySet())
		{
			next.computeIfAbsent(step.get(0), key -> new ArrayList<>()).add(step.get(1));
			next.computeIfAbsent(step.get(1), key -> new ArrayList<>()).add(step.get(0));
		}
		removeDetours(next);
		pruneSpurs(next);

		List<List<Long>> lines = new ArrayList<>();
		Set<List<Long>> walked = new HashSet<>();
		for (Map.Entry<Long, List<Long>> node : next.entrySet())
		{
			if (node.getValue().size() != 2)
			{
				for (long first : node.getValue())
				{
					walk(next, node.getKey(), first, walked).ifPresent(lines::add);
				}
			}
		}
		// circles without a junction
		for (Map.Entry<Long, List<Long>> node : next.entrySet())
		{
			for (long first : node.getValue())
			{
				walk(next, node.getKey(), first, walked).ifPresent(lines::add);
			}
		}
		List<FrequencyLine> streets = new ArrayList<>();
		for (List<Long> line : lines)
		{
			for (List<Long> piece : byPasses(line))
			{
				streets.add(simplify(piece, tolerance));
			}
		}
		return streets;
	}

	/**
	 * Removes the steps of few drives that a short detour over steps of more
	 * drives replaces, the fewest first: the triangles and squares of drives that
	 * went from cell to cell a little differently
	 */
	private void removeDetours(Map<Long, List<Long>> next)
	{
		List<Map.Entry<List<Long>, Integer>> byDrives = new ArrayList<>(steps.entrySet());
		byDrives.sort(Map.Entry.comparingByValue());
		for (Map.Entry<List<Long>, Integer> entry : byDrives)
		{
			long a = entry.getKey().get(0);
			long b = entry.getKey().get(1);
			next.get(a).remove(b);
			next.get(b).remove(a);
			if (!detour(next, a, b, entry.getValue()))
			{
				next.get(a).add(b);
				next.get(b).add(a);
			} else
			{
				steps.remove(entry.getKey());
			}
		}
		next.values().removeIf(List::isEmpty);
		next.keySet().removeIf(node -> !next.containsKey(node));
	}

	/**
	 * @return whether a and b are connected in at most {@link #DETOUR} steps that
	 *         as many drives took at least
	 */
	private boolean detour(Map<Long, List<Long>> next, long a, long b, int drives)
	{
		Set<Long> reached = new HashSet<>(List.of(a));
		List<Long> front = List.of(a);
		for (int depth = 0; depth < DETOUR && !front.isEmpty(); depth++)
		{
			List<Long> following = new ArrayList<>();
			for (long node : front)
			{
				for (long other : next.get(node))
				{
					if (steps.getOrDefault(step(node, other), 0) < drives || !reached.add(other))
					{
						continue;
					}
					if (other == b)
					{
						return true;
					}
					following.add(other);
				}
			}
			front = following;
		}
		return false;
	}

	/**
	 * @return the street cut where the passes of its cells change, e.g. where a
	 *         drive turned in; the pieces meet at a cell
	 */
	private List<List<Long>> byPasses(List<Long> street)
	{
		int size = street.size();
		int[] passes = passes(street);
		// a cell at the edge of the cells of another drive doesn't cut the street
		int[] smoothed = new int[size];
		for (int i = 0; i < size; i++)
		{
			int[] window = Arrays.copyOfRange(passes, Math.max(0, i - SMOOTHING), Math.min(size, i + SMOOTHING + 1));
			Arrays.sort(window);
			smoothed[i] = window[window.length / 2];
		}
		List<List<Long>> pieces = new ArrayList<>();
		int start = 0;
		for (int i = 1; i <= size; i++)
		{
			if (i == size || smoothed[i] != smoothed[start])
			{
				// up to the first cell of the next piece, so they meet
				int end = Math.min(i, size - 1);
				if (end > start)
				{
					pieces.add(street.subList(start, end + 1));
				}
				start = i;
			}
		}
		return pieces;
	}

	/**
	 * @return the passes of each cell of the street in its direction there, the
	 *         passes of all drives within {@link #NEAR_AXES} of it
	 */
	private int[] passes(List<Long> street)
	{
		int[] passes = new int[street.size()];
		for (int i = 0; i < street.size(); i++)
		{
			// a cell of the network was driven, at a sharp corner perhaps in another axis
			passes[i] = Math.max(1, cells.get(street.get(i)).passes[axisAt(street, i)]);
		}
		return passes;
	}

	/** of the street at its cell, from the cell before to the one after */
	private int axisAt(List<Long> street, int i)
	{
		Cell before = cells.get(street.get(Math.max(0, i - 1)));
		Cell after = cells.get(street.get(Math.min(street.size() - 1, i + 1)));
		return SharedLine.axis(after.x / after.samples - before.x / before.samples,
				after.y / after.samples - before.y / before.samples);
	}

	/**
	 * A dead end of a few cells at a street is a sample that came off it, e.g. at
	 * a junction: it would cut the street into pieces
	 */
	private static void pruneSpurs(Map<Long, List<Long>> next)
	{
		for (long end : List.copyOf(next.keySet()))
		{
			List<Long> neighbors = next.get(end);
			if (neighbors == null || neighbors.size() != 1)
			{
				continue;
			}
			List<Long> path = new ArrayList<>(List.of(end, neighbors.getFirst()));
			while (next.get(path.getLast()).size() == 2 && path.size() <= SPUR)
			{
				List<Long> around = next.get(path.getLast());
				long before = path.get(path.size() - 2);
				path.add(around.get(0) != before ? around.get(0) : around.get(1));
			}
			if (next.get(path.getLast()).size() < 3 || path.size() - 1 > SPUR)
			{
				continue;
			}
			for (int i = 1; i < path.size(); i++)
			{
				next.get(path.get(i - 1)).remove(path.get(i));
				next.get(path.get(i)).remove(path.get(i - 1));
			}
			path.subList(0, path.size() - 1).forEach(next::remove);
		}
	}

	/**
	 * @return the cells from a node over the first one to the next junction or
	 *         end, empty when the way was walked already
	 */
	private Optional<List<Long>> walk(Map<Long, List<Long>> next, long start, long first,
			Set<List<Long>> walked)
	{
		if (!walked.add(step(start, first)))
		{
			return Optional.empty();
		}
		List<Long> cellsOfStreet = new ArrayList<>(List.of(start, first));
		long previous = start;
		long node = first;
		while (next.get(node).size() == 2 && node != start)
		{
			List<Long> neighbors = next.get(node);
			long following = neighbors.get(0) != previous ? neighbors.get(0) : neighbors.get(1);
			if (!walked.add(step(node, following)))
			{
				break;
			}
			previous = node;
			node = following;
			cellsOfStreet.add(node);
		}
		return Optional.of(cellsOfStreet);
	}

	private static List<Long> step(long a, long b)
	{
		return a < b ? List.of(a, b) : List.of(b, a);
	}

	/**
	 * @return the street at the averages of its cells, with the passes of most of
	 *         them and when the car passed it last
	 */
	private FrequencyLine simplify(List<Long> street, double tolerance)
	{
		int[] passes = passes(street);
		long last = 0;
		List<TrackPoint> points = new ArrayList<>();
		for (int i = 0; i < street.size(); i++)
		{
			Cell cell = cells.get(street.get(i));
			last = Math.max(last, cell.last);
			double x = cell.x / cell.samples;
			double y = cell.y / cell.samples;
			points.add(new TrackPoint(null, Math.toDegrees(y / TrackSimplifier.EARTH_RADIUS) + lat0,
					Math.toDegrees(x / (TrackSimplifier.EARTH_RADIUS * cosLat)) + lon0, null));
		}
		Arrays.sort(passes);
		List<TrackPoint> kept = TrackSimplifier.simplify(points, tolerance, Double.POSITIVE_INFINITY);
		double[][] coordinates = new double[kept.size()][];
		for (int i = 0; i < kept.size(); i++)
		{
			coordinates[i] = new double[] {
					TrackSegment.round(kept.get(i).lon(), 1e6), TrackSegment.round(kept.get(i).lat(), 1e6)
			};
		}
		return new FrequencyLine(passes[passes.length / 2],
				OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(last), java.time.ZoneOffset.UTC), coordinates);
	}

	/** meters east and north of the first point, flat like the simplification */
	private double[] project(TrackPoint point)
	{
		if (cosLat == 0)
		{
			lat0 = point.lat();
			lon0 = point.lon();
			cosLat = Math.cos(Math.toRadians(lat0));
		}
		return new double[] {
				Math.toRadians(point.lon() - lon0) * TrackSimplifier.EARTH_RADIUS * cosLat,
				Math.toRadians(point.lat() - lat0) * TrackSimplifier.EARTH_RADIUS
		};
	}

	private static long cell(double meters)
	{
		return (long) Math.floor(meters / CELL);
	}

	private static long key(long column, long row)
	{
		return column << 32 | (row & 0xffffffffL);
	}
}
