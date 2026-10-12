package de.codeflowwizardry.carledger.rest.car.reminder;

import java.time.LocalDate;

/**
 * @param date
 *            when it was done, today if empty
 * @param odometer
 *            km when it was done, the current odometer of the car if empty
 */
public record ReminderDoneInput(LocalDate date, Integer odometer)
{
}
