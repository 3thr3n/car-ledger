package de.codeflowwizardry.carledger.rest.car.reminder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A reminder is due on a date, at an odometer reading or at whichever comes
 * first.
 *
 * @param intervalMonths
 *            repeats this many months after it is done, needs a due date
 * @param intervalKm
 *            repeats this many km after it is done, needs a due odometer
 */
public record ReminderInput(String title, String note, LocalDate dueDate, Integer dueOdometer,
		Integer intervalMonths, Integer intervalKm)
{
	public static final int MAX_ODOMETER = 10_000_000;

	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (title == null || title.isBlank())
		{
			errors.add("Title must be set!");
		} else if (title.length() > 100)
		{
			errors.add("Title must not be longer than 100 characters!");
		}
		if (note != null && note.length() > 500)
		{
			errors.add("Note must not be longer than 500 characters!");
		}
		if (dueDate == null && dueOdometer == null)
		{
			errors.add("Due date or due odometer must be set!");
		}
		if (dueOdometer != null && (dueOdometer < 0 || dueOdometer > MAX_ODOMETER))
		{
			errors.add("Due odometer must be between 0 and " + MAX_ODOMETER + "!");
		}
		if (intervalMonths != null)
		{
			if (intervalMonths < 1 || intervalMonths > 120)
			{
				errors.add("Interval in months must be between 1 and 120!");
			}
			if (dueDate == null)
			{
				errors.add("Interval in months needs a due date!");
			}
		}
		if (intervalKm != null)
		{
			if (intervalKm < 1 || intervalKm > 1_000_000)
			{
				errors.add("Interval in km must be between 1 and 1000000!");
			}
			if (dueOdometer == null)
			{
				errors.add("Interval in km needs a due odometer!");
			}
		}
		return errors;
	}
}
