package de.codeflowwizardry.carledger.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.car.track.GForces;
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;

class RoadTest
{
	private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-06T08:00:00Z");

	private final List<TrackPoint> points = new ArrayList<>();
	private final Road road = new Road(points::add);

	@Test
	void shouldPutTheVerticesBetweenThePoints()
	{
		TrackPoint first = new TrackPoint(START, 48.0, 11.0, 40.0);
		GForces g = new GForces(null, 0.3, -0.2, true);
		TrackPoint second = new TrackPoint(START.plusSeconds(4), 48.0, 11.002, 60.0, g);
		road.accept(first, null);
		// a quarter and three quarters of the way along a straight road
		road.accept(second, Polyline.encode(new double[] {
				48.0, 11.0005, 48.0, 11.0015
		}));

		assertEquals(4, points.size());
		assertSame(first, points.get(0));
		assertSame(second, points.get(3));
		TrackPoint vertex = points.get(1);
		assertEquals(START.plusSeconds(1), vertex.ts());
		assertEquals(11.0005, vertex.lon(), 1e-9);
		assertEquals(45.0, vertex.speed(), 1e-6);
		// the peaks of the piece the vertex lies on
		assertSame(g, vertex.g());
		assertEquals(START.plusSeconds(3), points.get(2).ts());
		assertEquals(55.0, points.get(2).speed(), 1e-6);
	}

	@Test
	void shouldKeepTheSpeedOfThePointWithoutTheOneBefore()
	{
		road.accept(new TrackPoint(START, 48.0, 11.0, null), null);
		road.accept(new TrackPoint(START.plusSeconds(2), 48.0, 11.001, 30.0),
				Polyline.encode(new double[] {
						48.0001, 11.0005
				}));

		assertEquals(30.0, points.get(1).speed());
	}

	@Test
	void shouldNotDrawARoadFromBeforeTheRangeOrOverAGap()
	{
		String vertex = Polyline.encode(new double[] {
				48.0001, 11.0005
		});
		// the point before it isn't read
		road.accept(new TrackPoint(START, 48.0, 11.0, null), vertex);
		// a new segment
		road.accept(new TrackPoint(START.plus(TrackService.GAP).plusSeconds(1), 48.0, 11.001, null), vertex);
		// not a polyline: the straight line
		road.accept(new TrackPoint(START.plus(TrackService.GAP).plusSeconds(2), 48.0, 11.002, null), "_p~iF~ps|U_");

		assertEquals(3, points.size());
		assertNull(points.get(1).speed());
	}
}
