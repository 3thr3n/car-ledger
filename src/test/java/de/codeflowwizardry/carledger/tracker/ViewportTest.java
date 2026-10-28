package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;
import jakarta.ws.rs.BadRequestException;

class ViewportTest
{
	private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-20T08:00:00Z");

	@Test
	void shouldBeTheWholeTrackWithoutBboxAndZoom()
	{
		assertNull(Viewport.of(null, null));
	}

	@Test
	void shouldReadTheBbox()
	{
		assertEquals(new Viewport(9.5, 49.5, 10.5, 50.5, 14.0), Viewport.of(" 9.5,49.5, 10.5,50.5", 14.0));
	}

	@Test
	void shouldRejectAnIncompleteOrInvalidViewport()
	{
		for (String bbox : List.of("9.5,49.5,10.5", "west,49.5,10.5,50.5", "10.5,49.5,9.5,50.5", "9.5,-91,10.5,50.5",
				"-181,49.5,10.5,50.5"))
		{
			assertThrows(BadRequestException.class, () -> Viewport.of(bbox, 14.0), bbox);
		}
		assertThrows(BadRequestException.class, () -> Viewport.of("9.5,49.5,10.5,50.5", null));
		assertThrows(BadRequestException.class, () -> Viewport.of(null, 14.0));
		assertThrows(BadRequestException.class, () -> Viewport.of("9.5,49.5,10.5,50.5", 25.0));
		assertThrows(BadRequestException.class, () -> Viewport.of("9.5,49.5,10.5,50.5", Double.NaN));
	}

	@Test
	void shouldHalveThePixelWithEachZoom()
	{
		// tiles of 512 pixels: 78 km per pixel at zoom 0 on the equator (mean radius)
		assertEquals(78_184, new Viewport(-1, -1, 1, 1, 0).metersPerPixel(), 5);
		Viewport city = new Viewport(9.9, 49.9, 10.1, 50.1, 15);
		assertEquals(1.54, city.metersPerPixel(), 0.01);
		assertEquals(city.metersPerPixel() / 2, new Viewport(9.9, 49.9, 10.1, 50.1, 16).metersPerPixel(), 1e-9);
	}

	@Test
	void shouldSimplifyToAPixelButNotFinerThanAMeterOrCoarserThanTheRange()
	{
		Duration days = Duration.ofDays(3);
		assertEquals(15, TrackService.tolerance(days, null));
		assertEquals(1.54, TrackService.tolerance(days, new Viewport(9.9, 49.9, 10.1, 50.1, 15)), 0.01);
		assertEquals(TrackService.MIN_TOLERANCE, TrackService.tolerance(days, new Viewport(9.9, 49.9, 10.1, 50.1, 20)));
		assertEquals(15, TrackService.tolerance(days, new Viewport(9.9, 49.9, 10.1, 50.1, 5)));
	}

	@Test
	void shouldPassOnThePointsWithinAndCutWhereTheTrackLeftIt()
	{
		Viewport view = new Viewport(9.9, 49.9, 10.1, 50.1, 14);
		List<String> seen = new ArrayList<>();
		Consumer<TrackPoint> filter = view.filter(point -> seen.add(String.valueOf(point.lon())),
				() -> seen.add("cut"));

		for (double lon : new double[] {
				9.8, 9.95, 10.0, 10.2, 10.3, 10.05, 10.06
		})
		{
			filter.accept(new TrackPoint(START, 50, lon, null));
		}

		assertEquals(List.of("9.95", "10.0", "cut", "10.05", "10.06"), seen);
	}
}
