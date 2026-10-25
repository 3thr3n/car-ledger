package de.codeflowwizardry.carledger.rest.car.inspection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.codeflowwizardry.carledger.data.InspectionResult;
import de.codeflowwizardry.carledger.rest.car.reminder.ReminderInput;

/**
 * @param testingBody
 *            e.g. TÜV, DEKRA, GTÜ
 * @param nextDue
 *            any day of the month the next HU is due, after the date
 */
public record TechnicalInspectionInput(LocalDate date, Integer odometer, String testingBody, InspectionResult result,
		String defects, LocalDate nextDue)
{
	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (date == null)
		{
			errors.add("Date must be set!");
		} else if (date.isAfter(LocalDate.now()))
		{
			errors.add("Date must not be in the future!");
		}
		if (result == null)
		{
			errors.add("Result must be set!");
		}
		if (odometer != null && (odometer < 0 || odometer > ReminderInput.MAX_ODOMETER))
		{
			errors.add("Odometer must be between 0 and " + ReminderInput.MAX_ODOMETER + "!");
		}
		if (testingBody != null && testingBody.strip().length() > 100)
		{
			errors.add("Testing body must not be longer than 100 characters!");
		}
		if (defects != null && defects.strip().length() > 2000)
		{
			errors.add("Defects must not be longer than 2000 characters!");
		}
		if (date != null && nextDue != null && !nextDue.withDayOfMonth(1).isAfter(date.withDayOfMonth(1)))
		{
			errors.add("Next due month must be after the month of the inspection!");
		}
		return errors;
	}
}
