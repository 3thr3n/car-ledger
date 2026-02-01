package de.codeflowwizardry.carledger.rest.records.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Statistics for total of:
 * <li>Units (liters/gallons)
 * <li>Distance
 * <li>Fuel cost
 * <li>Maintenance cost
 * <li>Total cost (fuel + maintenance)
 * <li>Number of fuel bills
 * <li>Number of maintenance bills
 */
public record TotalStats(BigDecimal unit, BigDecimal trackedDistance, BigDecimal fuelTotal, BigDecimal maintenanceTotal,
		BigDecimal total, Integer fuelBills, Integer maintenanceBills)
{
	public TotalStats(BigDecimal unit, BigDecimal trackedDistance, BigDecimal fuelTotal, BigDecimal maintenanceTotal,
			BigDecimal total, Integer fuelBills, Integer maintenanceBills)
	{
		this.unit = unit;
		this.trackedDistance = trackedDistance;
		this.fuelTotal = fuelTotal;
		this.maintenanceTotal = maintenanceTotal;
		this.total = total;
		this.fuelBills = fuelBills;
		this.maintenanceBills = maintenanceBills;
	}

	public TotalStats()
	{
		this(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
	}

	@Override
	public BigDecimal unit()
	{
		return unit.setScale(2, RoundingMode.HALF_UP);
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
	public BigDecimal maintenanceTotal()
	{
		return maintenanceTotal.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal total()
	{
		return total.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public Integer fuelBills()
	{
		return fuelBills;
	}

	@Override
	public Integer maintenanceBills()
	{
		return maintenanceBills;
	}
}
