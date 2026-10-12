package de.codeflowwizardry.carledger.rest.car.reminder;

import java.time.LocalDate;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @param odometer
 *            current odometer of the car, see
 *            {@link de.codeflowwizardry.carledger.OdometerService}
 * @param daysLeft
 *            negative when overdue, missing without a due date
 * @param kmLeft
 *            negative when overdue, missing without a due odometer
 */
public record Reminder(
		@Schema(required = true) long id,
		@Schema(required = true) long carId,
		@Schema(required = true) String carName,
		@Schema(required = true) String title,
		String note,
		LocalDate dueDate,
		Integer dueOdometer,
		Integer intervalMonths,
		Integer intervalKm,
		@Schema(required = true) ReminderStatus status,
		@Schema(required = true) int odometer,
		Long daysLeft,
		Integer kmLeft)
{
}
