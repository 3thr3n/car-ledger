package de.codeflowwizardry.carledger.tracker;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

/**
 * Groups the stops of the car into places: a stop within {@link #RADIUS} of a
 * place is another visit of it, more than the drift of the GPS between two
 * stops at the same fuel station or parking lot. The middle of a place follows
 * its stops.
 */
final class PlaceFinder
{
	/** meters around the middle of a place that still are the place */
	static final double RADIUS = 75;

	/**
	 * @param lat
	 *            the middle of the stops
	 * @param lon
	 *            the middle of the stops
	 * @param last
	 *            the latest stop
	 */
	record Visits(double lat, double lon, int count, OffsetDateTime last)
	{
		TrackPoint point()
		{
			return new TrackPoint(last, lat, lon, null);
		}
	}

	private final List<Visits> places = new ArrayList<>();

	/**
	 * @param stop
	 *            where the car stopped, passed in the order of time
	 */
	void add(TrackPoint stop)
	{
		int nearest = -1;
		double distance = RADIUS;
		for (int i = 0; i < places.size(); i++)
		{
			double d = TrackSimplifier.distance(places.get(i).point(), stop);
			if (d <= distance)
			{
				nearest = i;
				distance = d;
			}
		}
		if (nearest < 0)
		{
			places.add(new Visits(stop.lat(), stop.lon(), 1, stop.ts()));
			return;
		}
		Visits place = places.get(nearest);
		int count = place.count() + 1;
		places.set(nearest, new Visits(place.lat() + (stop.lat() - place.lat()) / count,
				place.lon() + (stop.lon() - place.lon()) / count, count, stop.ts()));
	}

	/**
	 * @return the places in the order of their first stop
	 */
	List<Visits> places()
	{
		return List.copyOf(places);
	}
}
