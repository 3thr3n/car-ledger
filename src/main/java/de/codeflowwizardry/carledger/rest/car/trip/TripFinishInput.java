package de.codeflowwizardry.carledger.rest.car.trip;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import de.codeflowwizardry.carledger.rest.car.reminder.ReminderInput;

/**
 * The details that were not given at the start can be added, the given ones
 * can be repeated but not changed.
 *
 * @param endOdometer
 *            in km
 * @param endedAt
 *            time of the device
 * @param queued
 *            see {@link TripStartInput#queued()}
 */
public record TripFinishInput(Integer endOdometer, OffsetDateTime endedAt, String destination, String purpose,
		String businessPartner, boolean queued) implements TripDetails
{
	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (endOdometer == null)
		{
			errors.add("End odometer must be set!");
		} else if (endOdometer < 0 || endOdometer > ReminderInput.MAX_ODOMETER)
		{
			errors.add("End odometer must be between 0 and " + ReminderInput.MAX_ODOMETER + "!");
		}
		if (endedAt == null)
		{
			errors.add("End time must be set!");
		}
		validateDetails(errors);
		return errors;
	}
}
