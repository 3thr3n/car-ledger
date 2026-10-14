package de.codeflowwizardry.carledger.rest.car.stats;

import java.math.BigDecimal;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Costs, liters and kWh of one month. The averages are missing in months
 * without refuels or charges with a distance.
 *
 * @param distance
 *            tracked distance in km, per car the larger one of the refuels and
 *            the charges
 * @param unit
 *            liters
 * @param chargedUnit
 *            kWh
 * @param pricePerUnit
 *            average price per liter in cent, weighted by the liters
 * @param chargingPricePerUnit
 *            average price per kWh in cent, weighted by the kWh
 * @param consumption
 *            liters per 100 km
 * @param chargingConsumption
 *            kWh per 100 km
 * @param costPerKm
 *            all costs of the month per km in cent
 */
public record MonthStats(
		@Schema(examples = "2026-03", required = true) String month,
		@Schema(required = true) BigDecimal fuel,
		@Schema(required = true) BigDecimal charging,
		@Schema(required = true) BigDecimal maintenance,
		@Schema(required = true) BigDecimal miscellaneous,
		@Schema(required = true) BigDecimal recurring,
		@Schema(required = true) BigDecimal total,
		@Schema(required = true) BigDecimal distance,
		@Schema(required = true) BigDecimal unit,
		@Schema(required = true) BigDecimal chargedUnit,
		BigDecimal pricePerUnit,
		BigDecimal chargingPricePerUnit,
		BigDecimal consumption,
		BigDecimal chargingConsumption,
		BigDecimal costPerKm)
{
}
