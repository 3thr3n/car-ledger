package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Current odometer of a car: the latest reading plus the distance of the
 * refuels after it.
 *
 * @param value
 *            current odometer in km, never below a reading
 * @param reading
 *            latest reading, of the car, of a maintenance bill or of a trip
 * @param readingDate
 *            day of the reading
 * @param driven
 *            distance of the refuels after the reading in km, 0 if the value is
 *            the reading itself
 * @param perDay
 *            average km a day, from a reading about 90 days before the latest
 *            data; none without a reading at least 30 days before it
 * @param estimatedToday
 *            the value plus the average a day for the days since the latest
 *            reading or refuel, at most 60 days; only a guess, the value stays
 *            the odometer of the recorded data
 */
public record Odometer(
		@Schema(required = true) int value,
		@Schema(required = true) int reading,
		@Schema(required = true) LocalDate readingDate,
		@Schema(required = true) int driven,
		BigDecimal perDay,
		Integer estimatedToday)
{
}
