package de.codeflowwizardry.carledger.rest.car.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Averages of the period, refuels and charges without a distance are left out
 * of the consumption and cost per km
 *
 * @param pricePerUnit
 *            cent per liter
 * @param chargingPricePerUnit
 *            cent per kWh
 * @param distance
 *            km per refuel
 * @param fuelCostPerKm
 *            cent of the refuels per km
 * @param chargingCostPerKm
 *            cent of the charges per km
 * @param fuelConsumption
 *            liters per 100 km
 * @param chargingConsumption
 *            kWh per 100 km
 * @param maintenanceCost
 *            per maintenance
 * @param costPerKm
 *            all costs per tracked km in cent
 */
public record AverageStats(BigDecimal pricePerUnit, BigDecimal chargingPricePerUnit, BigDecimal distance,
		BigDecimal fuelCostPerKm, BigDecimal chargingCostPerKm, BigDecimal fuelConsumption,
		BigDecimal chargingConsumption, BigDecimal maintenanceCost, BigDecimal costPerKm)
{
	@Override
	public BigDecimal pricePerUnit()
	{
		return pricePerUnit.setScale(1, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal chargingPricePerUnit()
	{
		return chargingPricePerUnit.setScale(1, RoundingMode.HALF_UP);
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
	public BigDecimal chargingCostPerKm()
	{
		return chargingCostPerKm.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal fuelConsumption()
	{
		return fuelConsumption.setScale(2, RoundingMode.HALF_UP);
	}

	@Override
	public BigDecimal chargingConsumption()
	{
		return chargingConsumption.setScale(2, RoundingMode.HALF_UP);
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
