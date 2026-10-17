package de.codeflowwizardry.carledger.rest.records.input;

import java.math.BigDecimal;

/**
 * Input of a refuel or a charge, two of total, unit and price per unit are
 * enough
 */
public interface EnergyBillInput
{
	BigDecimal getTotal();

	/**
	 * @return liters or kWh
	 */
	BigDecimal getUnit();

	/**
	 * @return in cent per liter or kWh
	 */
	BigDecimal getPricePerUnit();

	/**
	 * @return in km since the last one of the same kind
	 */
	BigDecimal getDistance();
}
