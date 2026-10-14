package de.codeflowwizardry.carledger.data;

import java.math.BigDecimal;

/**
 * A refuel or a charge: an amount of energy for a price, with the distance
 * driven since the last one of the same kind
 */
public interface EnergyBill
{
	BillEntity getBill();

	/**
	 * @return liters or kWh
	 */
	BigDecimal getUnit();

	/**
	 * @return in cent per liter or kWh
	 */
	BigDecimal getPricePerUnit();

	/**
	 * @return in km
	 */
	BigDecimal getDistance();

	boolean isDistanceMissing();

	/**
	 * @param avgConsumption
	 *            liters or kWh per 100 km
	 */
	void setAvgConsumption(BigDecimal avgConsumption);

	/**
	 * @param costPerKm
	 *            in cent
	 */
	void setCostPerKm(BigDecimal costPerKm);
}
