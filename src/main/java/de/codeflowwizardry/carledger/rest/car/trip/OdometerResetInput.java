package de.codeflowwizardry.carledger.rest.car.trip;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.codeflowwizardry.carledger.rest.car.reminder.ReminderInput;

/**
 * @param resetAt
 *            time of the device
 * @param odometer
 *            the new reading in km, the base from then on
 * @param odometerBefore
 *            the last reading before in km, if it was still shown
 * @param reason
 *            e.g. the replaced instrument cluster
 */
public record OdometerResetInput(OffsetDateTime resetAt, Integer odometer, Integer odometerBefore, String reason)
{
	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (resetAt == null)
		{
			errors.add("Time must be set!");
		}
		if (odometer == null)
		{
			errors.add("Odometer must be set!");
		} else if (odometer < 0 || odometer > ReminderInput.MAX_ODOMETER)
		{
			errors.add("Odometer must be between 0 and " + ReminderInput.MAX_ODOMETER + "!");
		}
		if (odometerBefore != null && (odometerBefore < 0 || odometerBefore > ReminderInput.MAX_ODOMETER))
		{
			errors.add("Odometer before must be between 0 and " + ReminderInput.MAX_ODOMETER + "!");
		}
		if (StringUtils.isBlank(reason))
		{
			errors.add("A reason must be given!");
		} else if (reason.strip().length() > TripDeleteInput.MAX_REASON)
		{
			errors.add("The reason must not be longer than " + TripDeleteInput.MAX_REASON + " characters!");
		}
		return errors;
	}
}
