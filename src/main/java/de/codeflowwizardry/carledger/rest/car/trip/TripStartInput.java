package de.codeflowwizardry.carledger.rest.car.trip;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import de.codeflowwizardry.carledger.data.TripType;
import de.codeflowwizardry.carledger.rest.car.reminder.ReminderInput;

/**
 * @param startOdometer
 *            in km
 * @param startedAt
 *            time of the device
 * @param queued
 *            sent later from the offline queue: a trip that doesn't fit the
 *            other data is stored for a review instead of being rejected
 */
public record TripStartInput(TripType type, Integer startOdometer, OffsetDateTime startedAt, String destination,
		String purpose, String businessPartner, boolean queued) implements TripDetails
{
	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (type == null)
		{
			errors.add("Type must be set!");
		}
		if (startOdometer == null)
		{
			errors.add("Start odometer must be set!");
		} else if (startOdometer < 0 || startOdometer > ReminderInput.MAX_ODOMETER)
		{
			errors.add("Start odometer must be between 0 and " + ReminderInput.MAX_ODOMETER + "!");
		}
		if (startedAt == null)
		{
			errors.add("Start time must be set!");
		}
		validateDetails(errors);
		return errors;
	}
}
