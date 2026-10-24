package de.codeflowwizardry.carledger.rest.car.modification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.codeflowwizardry.carledger.data.ApprovalType;
import de.codeflowwizardry.carledger.rest.car.reminder.ReminderInput;

/**
 * A modification is removed with a removed date, it stays in the history.
 *
 * @param approvalNumber
 *            e.g. the KBA number
 * @param conditions
 *            the Auflagen of the approval
 * @param inspectionDate
 *            of the Abnahme
 * @param testingBody
 *            of the Abnahme, e.g. TÜV, DEKRA, GTÜ
 * @param registered
 *            entered in the Zulassungsbescheinigung Teil I
 * @param removedOdometer
 *            needs a removed date
 * @param billId
 *            of the part or the installation, an entry of the same car
 */
public record ModificationInput(String part, String manufacturer, ApprovalType approvalType, String approvalNumber,
		String conditions, LocalDate inspectionDate, String testingBody, boolean registered, LocalDate installedDate,
		Integer installedOdometer, LocalDate removedDate, Integer removedOdometer, String notes, Long billId)
{
	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (part == null || part.isBlank())
		{
			errors.add("Part must be set!");
		}
		length(errors, "Part", part, 100);
		length(errors, "Manufacturer", manufacturer, 100);
		length(errors, "Approval number", approvalNumber, 50);
		length(errors, "Conditions", conditions, 2000);
		length(errors, "Testing body", testingBody, 100);
		length(errors, "Notes", notes, 1000);
		if (approvalType == null)
		{
			errors.add("Approval type must be set!");
		}
		odometer(errors, "Installed odometer", installedOdometer);
		odometer(errors, "Removed odometer", removedOdometer);
		if (removedOdometer != null && removedDate == null)
		{
			errors.add("Removed odometer needs a removed date!");
		}
		if (installedDate != null && removedDate != null && removedDate.isBefore(installedDate))
		{
			errors.add("Removed date must not be before the installed date!");
		}
		if (installedOdometer != null && removedOdometer != null && removedOdometer < installedOdometer)
		{
			errors.add("Removed odometer must not be below the installed odometer!");
		}
		return errors;
	}

	private static void length(List<String> errors, String name, String value, int max)
	{
		if (value != null && value.strip().length() > max)
		{
			errors.add(name + " must not be longer than " + max + " characters!");
		}
	}

	private static void odometer(List<String> errors, String name, Integer value)
	{
		if (value != null && (value < 0 || value > ReminderInput.MAX_ODOMETER))
		{
			errors.add(name + " must be between 0 and " + ReminderInput.MAX_ODOMETER + "!");
		}
	}
}
