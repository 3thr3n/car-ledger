package de.codeflowwizardry.carledger.rest.car.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Totals of the period
 *
 * @param unit
 *            liters of the refuels
 * @param chargedUnit
 *            kWh of the charges
 * @param trackedDistance
 *            km, per car the larger distance of the refuels and the charges
 * @param total
 *            all costs, including the recurring ones
 */
public record TotalStats(BigDecimal unit, BigDecimal chargedUnit, BigDecimal trackedDistance, BigDecimal fuelTotal,
		BigDecimal chargingTotal, BigDecimal maintenanceTotal, BigDecimal miscellaneousTotal, BigDecimal total,
		Integer fuelBills, Integer chargingBills, Integer maintenanceBills, Integer miscellaneousBills)
{
	@Override
	public BigDecimal unit()
	{
		return unit.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal chargedUnit()
	{
		return chargedUnit.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal trackedDistance()
	{
		return trackedDistance.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal fuelTotal()
	{
		return fuelTotal.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal chargingTotal()
	{
		return chargingTotal.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal maintenanceTotal()
	{
		return maintenanceTotal.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal miscellaneousTotal()
	{
		return miscellaneousTotal.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal total()
	{
		return total.setScale(2, RoundingMode.HALF_UP);
	}
}
