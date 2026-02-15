package de.codeflowwizardry.carledger.rest.car.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Statistics for average of:
 * <li>Price per unit
 * <li>Distance per fill-up
 * <li>Cost per km
 * <li>Fuel consumption (L/100km)
 * <li>Maintenance cost per bill
 * <li>Cost per km (total, including maintenance)
 */
public record AverageStats(BigDecimal pricePerUnit, BigDecimal distance, BigDecimal fuelCostPerKm,
		BigDecimal fuelConsumption, BigDecimal maintenanceCost, BigDecimal costPerKm)
{
	@Override
	public BigDecimal pricePerUnit()
	{
		return pricePerUnit.setScale(1, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal distance()
	{
		return distance.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal fuelCostPerKm()
	{
		return fuelCostPerKm.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal fuelConsumption()
	{
		return fuelConsumption.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal maintenanceCost()
	{
		return maintenanceCost.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal costPerKm()
	{
		return costPerKm.setScale(2, RoundingMode.HALF_UP);
	}
}
