package de.codeflowwizardry.carledger.rest.records.stats;

/**
 * Statistics for high/low of:
 * <li>Price per unit
 * <li>Distance per fill-up
 * <li>Cost per km
 * <li>Fuel consumption
 * <li>Fuel bill cost
 * <li>Maintenance bill cost
 */
public record HiLoStats(HiLo pricePerUnit, HiLo distance, HiLo fuelCostPerKm, HiLo consumption, HiLo fuelCost,
		HiLo maintenanceCost)
{
}
