package de.codeflowwizardry.carledger.rest.records;

import java.time.LocalDate;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Current odometer of a car: the latest reading plus the distance of the
 * refuels after it.
 *
 * @param value
 *            current odometer in km, never below a reading
 * @param reading
 *            latest reading, of the car or of a maintenance bill
 * @param readingDate
 *            day of the reading
 * @param driven
 *            distance of the refuels after the reading in km, 0 if the value is
 *            the reading itself
 */
public record Odometer(
		@Schema(required = true) int value,
		@Schema(required = true) int reading,
		@Schema(required = true) LocalDate readingDate,
		@Schema(required = true) int driven)
{
}
