package de.codeflowwizardry.carledger.rest.car.recurring;

import java.time.LocalDate;

/**
 * @param endDate
 *            first day without payments, payments due on or after it are
 *            removed
 */
public record RecurringBillEndInput(LocalDate endDate)
{
}
