package de.codeflowwizardry.carledger.rest.car.trip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.TripEntity;
import de.codeflowwizardry.carledger.data.TripType;

class TripTest
{
	final OffsetDateTime start = OffsetDateTime.parse("2026-10-19T08:00:00Z");

	private TripEntity trip(OffsetDateTime createdAt)
	{
		return new TripEntity(UUID.randomUUID(), null, null, TripType.PRIVATE, 1000, start, false, createdAt);
	}

	@Test
	void shouldNotMarkATripOfTheOfflineQueue()
	{
		TripEntity trip = trip(start.plusMinutes(90));
		trip.finish(1050, start.plusHours(1), start.plusHours(3));
		assertFalse(Trip.enteredLater(trip));
	}

	@Test
	void shouldMarkAStartReceivedMoreThanTwoHoursLater()
	{
		assertTrue(Trip.enteredLater(trip(start.plusHours(2).plusSeconds(1))));
		assertFalse(Trip.enteredLater(trip(start.plusHours(2))));
	}

	@Test
	void shouldMarkAFinishReceivedMoreThanTwoHoursLater()
	{
		TripEntity trip = trip(start.plusMinutes(1));
		trip.finish(1050, start.plusHours(1), start.plusHours(3).plusMinutes(1));
		assertTrue(Trip.enteredLater(trip));
	}
}
