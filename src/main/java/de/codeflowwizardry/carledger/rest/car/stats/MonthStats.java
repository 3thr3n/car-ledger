package de.codeflowwizardry.carledger.rest.car.stats;

import java.math.BigDecimal;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Costs and fuel of one month. The averages are missing in months without
 * refuels with a distance.
 *
 * @param distance
 *            tracked distance of the refuels in km
 * @param unit
 *            liters
 * @param pricePerUnit
 *            average price per liter in cent, weighted by the liters
 * @param consumption
 *            liters per 100 km
 * @param costPerKm
 *            all costs of the month per km in cent
 */
public record MonthStats(
		@Schema(example = "2026-03", required = true) String month,
		@Schema(required = true) BigDecimal fuel,
		@Schema(required = true) BigDecimal maintenance,
		@Schema(required = true) BigDecimal miscellaneous,
		@Schema(required = true) BigDecimal recurring,
		@Schema(required = true) BigDecimal total,
		@Schema(required = true) BigDecimal distance,
		@Schema(required = true) BigDecimal unit,
		BigDecimal pricePerUnit,
		BigDecimal consumption,
		BigDecimal costPerKm)
{
}
