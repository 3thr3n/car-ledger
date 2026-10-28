package de.codeflowwizardry.carledger.tracker;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

/**
 * The drives of the track the user selected on the map, each from its start to
 * its end (both inclusive). Without any, all points are selected.
 */
public final class Selection
{
	/** drives of one request */
	static final int MAX = 200;

	public static final Selection ALL = new Selection(List.of());

	private record Range(OffsetDateTime start, OffsetDateTime end)
	{
	}

	private final List<Range> ranges;

	private Selection(List<Range> ranges)
	{
		this.ranges = ranges;
	}

	/**
	 * @param drives
	 *            ISO-8601 intervals, <code>start/end</code>
	 * @throws jakarta.ws.rs.BadRequestException
	 *             if one isn't an interval, or for too many
	 */
	public static Selection of(List<String> drives)
	{
		if (drives == null || drives.isEmpty())
		{
			return ALL;
		}
		List<String> errors = new ArrayList<>();
		List<Range> ranges = new ArrayList<>();
		if (drives.size() > MAX)
		{
			errors.add("At most " + MAX + " drives!");
		}
		for (String drive : drives)
		{
			String[] ends = drive == null ? new String[0] : drive.split("/", -1);
			try
			{
				if (ends.length != 2)
				{
					throw new DateTimeParseException("no interval", String.valueOf(drive), 0);
				}
				Range range = new Range(OffsetDateTime.parse(ends[0]), OffsetDateTime.parse(ends[1]));
				if (range.end().isBefore(range.start()))
				{
					errors.add("A drive must not end before it starts: " + drive);
				}
				ranges.add(range);
			}
			catch (DateTimeParseException e)
			{
				errors.add("A drive is start/end, ISO-8601: " + drive);
			}
		}
		Utils.requireValid(errors);
		ranges.sort(Comparator.comparing(Range::start));
		return new Selection(List.copyOf(ranges));
	}

	public boolean isAll()
	{
		return ranges.isEmpty();
	}

	/**
	 * @return from the start of the first drive to the end of the last, the whole
	 *         range for all points
	 */
	Duration span(Duration whole)
	{
		if (isAll())
		{
			return whole;
		}
		OffsetDateTime end = ranges.stream().map(Range::end).max(Comparator.naturalOrder()).orElseThrow();
		return Duration.between(ranges.getFirst().start(), end);
	}

	/**
	 * A filter for one run over the points in the order of time
	 *
	 * @param next
	 *            gets the selected points
	 * @param cut
	 *            runs between two drives, so their points aren't joined
	 */
	Consumer<TrackPoint> filter(Consumer<TrackPoint> next, Runnable cut)
	{
		if (isAll())
		{
			return next;
		}
		return new Consumer<>() {
			/** of the range of the last point passed on */
			private int last = -1;

			@Override
			public void accept(TrackPoint point)
			{
				int index = indexOf(point.ts());
				if (index < 0)
				{
					return;
				}
				if (last >= 0 && index != last)
				{
					cut.run();
				}
				last = index;
				next.accept(point);
			}
		};
	}

	private int indexOf(OffsetDateTime ts)
	{
		for (int i = 0; i < ranges.size(); i++)
		{
			Range range = ranges.get(i);
			if (!ts.isBefore(range.start()) && !ts.isAfter(range.end()))
			{
				return i;
			}
		}
		return -1;
	}
}
