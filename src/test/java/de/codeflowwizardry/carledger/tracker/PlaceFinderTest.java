package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.PlaceEntity;
import de.codeflowwizardry.carledger.rest.car.track.Place;
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;
import de.codeflowwizardry.carledger.tracker.PlaceFinder.Visits;

class PlaceFinderTest
{
	private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-01T08:00:00Z");
	/** degrees of latitude per meter */
	private static final double LAT_PER_METER = 1 / 111_195.0;

	/** a stop the meters north of the station */
	private static TrackPoint stop(int day, double north)
	{
		return new TrackPoint(START.plusDays(day), 50 + north * LAT_PER_METER, 10, null);
	}

	@Test
	void shouldCountTheStopsNearEachOtherAsOnePlace()
	{
		PlaceFinder finder = new PlaceFinder();
		finder.add(stop(0, 0));
		finder.add(stop(1, 40));
		finder.add(stop(2, -30));
		finder.add(stop(3, 1_000));

		List<Visits> places = finder.places();
		assertEquals(2, places.size());
		Visits station = places.getFirst();
		assertEquals(3, station.count());
		assertEquals(START.plusDays(2), station.last());
		// the middle of the stops
		assertEquals(50 + 10 / 3.0 * LAT_PER_METER, station.lat(), 1e-9);
		assertEquals(1, places.get(1).count());
	}

	@Test
	void shouldStartAnotherPlaceBeyondTheRadius()
	{
		PlaceFinder finder = new PlaceFinder();
		finder.add(stop(0, 0));
		finder.add(stop(1, PlaceFinder.RADIUS + 5));

		assertEquals(2, finder.places().size());
	}

	@Test
	void shouldShowFrequentAndNamedPlaces()
	{
		CarEntity car = new CarEntity();
		List<Visits> found = List.of(
				new Visits(50, 10, 3, START),
				new Visits(50 + 1_000 * LAT_PER_METER, 10, 7, START.plusDays(1)),
				new Visits(50 + 2_000 * LAT_PER_METER, 10, 2, START.plusDays(2)));
		List<PlaceEntity> named = List.of(
				new PlaceEntity(car, 50 + 1_030 * LAT_PER_METER, 10, "Fuel station"),
				new PlaceEntity(car, 51, 11, "Holiday flat"));

		List<Place> places = Places.merge(found, named);

		assertEquals(3, places.size());
		// the most visited first, the named one where it was named
		assertEquals(new Place(null, "Fuel station", 50 + 1_030 * LAT_PER_METER, 10, 7, START.plusDays(1)),
				places.get(0));
		assertEquals(new Place(null, null, 50, 10, 3, START), places.get(1));
		Place holiday = places.get(2);
		assertEquals("Holiday flat", holiday.name());
		assertEquals(0, holiday.visits());
		assertNull(holiday.lastVisit());
	}
}
