package de.codeflowwizardry.carledger.rest.car.trip;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import de.codeflowwizardry.carledger.data.TripType;

/**
 * A forgotten trip, entered afterwards with its start and its finish
 *
 * @param startOdometer
 *            in km
 * @param endOdometer
 *            in km
 */
public record TripPastInput(TripType type, Integer startOdometer, OffsetDateTime startedAt, Integer endOdometer,
		OffsetDateTime endedAt, String destination, String purpose, String businessPartner)
{
	public TripStartInput start()
	{
		return new TripStartInput(type, startOdometer, startedAt, destination, purpose, businessPartner, false);
	}

	public TripFinishInput finish()
	{
		return new TripFinishInput(endOdometer, endedAt, null, null, null, false);
	}

	public List<String> validate()
	{
		List<String> errors = new ArrayList<>(start().validate());
		errors.addAll(finish().validate());
		if (errors.isEmpty())
		{
			if (endOdometer < startOdometer)
			{
				errors.add("End odometer must not be below the start!");
			}
			if (endedAt.isBefore(startedAt))
			{
				errors.add("End time must not be before the start!");
			}
		}
		return errors;
	}
}
