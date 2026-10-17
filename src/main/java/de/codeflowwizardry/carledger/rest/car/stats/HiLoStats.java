package de.codeflowwizardry.carledger.rest.car.stats;

/**
 * Lowest and highest value of the period
 *
 * @param pricePerUnit
 *            cent per liter
 * @param chargingPricePerUnit
 *            cent per kWh
 * @param distance
 *            km per refuel
 * @param fuelCostPerKm
 *            cent of a refuel per km
 * @param consumption
 *            liters per 100 km
 * @param chargingConsumption
 *            kWh per 100 km
 * @param fuelCost
 *            of a refuel
 * @param chargingCost
 *            of a charge
 * @param maintenanceCost
 *            of a maintenance
 */
public record HiLoStats(HiLo pricePerUnit, HiLo chargingPricePerUnit, HiLo distance, HiLo fuelCostPerKm,
		HiLo consumption, HiLo chargingConsumption, HiLo fuelCost, HiLo chargingCost, HiLo maintenanceCost)
{
}
