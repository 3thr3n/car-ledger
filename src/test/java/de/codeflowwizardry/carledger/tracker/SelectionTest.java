package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;
import jakarta.ws.rs.BadRequestException;

class SelectionTest
{
	private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-20T08:00:00Z");

	private static String drive(int fromSecond, int toSecond)
	{
		return START.plusSeconds(fromSecond) + "/" + START.plusSeconds(toSecond);
	}

	/** the seconds of the points passed on, -1 for a cut */
	private static List<Integer> run(Selection selection)
	{
		List<Integer> seen = new ArrayList<>();
		Consumer<TrackPoint> filter = selection.filter(
				point -> seen.add((int) Duration.between(START, point.ts()).toSeconds()), () -> seen.add(-1));
		for (int second = 0; second < 10; second++)
		{
			filter.accept(new TrackPoint(START.plusSeconds(second), 50, 10, null));
		}
		return seen;
	}

	@Test
	void shouldPassOnAllWithoutDrives()
	{
		assertSame(Selection.ALL, Selection.of(null));
		assertSame(Selection.ALL, Selection.of(List.of()));
		assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), run(Selection.ALL));
	}

	@Test
	void shouldPassOnTheDrivesWithBothEndsAndCutBetweenThem()
	{
		// right after each other: still two lines
		Selection selection = Selection.of(List.of(drive(6, 8), drive(1, 2), drive(3, 4)));

		assertEquals(List.of(1, 2, -1, 3, 4, -1, 6, 7, 8), run(selection));
	}

	@Test
	void shouldRejectWhatIsntADrive()
	{
		assertThrows(BadRequestException.class, () -> Selection.of(List.of("yesterday")));
		assertThrows(BadRequestException.class, () -> Selection.of(List.of(START.toString())));
		assertThrows(BadRequestException.class, () -> Selection.of(List.of(drive(5, 4))));
		assertThrows(BadRequestException.class,
				() -> Selection.of(Collections.nCopies(Selection.MAX + 1, drive(0, 1))));
		assertTrue(!Selection.of(Collections.nCopies(Selection.MAX, drive(0, 1))).isAll());
	}
}
