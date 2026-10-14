package de.codeflowwizardry.carledger.data.factory;

import static de.codeflowwizardry.carledger.StatsCalculator.ONE_HUNDRED;
import static de.codeflowwizardry.carledger.Utils.valueWasSet;

import java.math.BigDecimal;
import java.math.RoundingMode;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.EnergyBill;
import de.codeflowwizardry.carledger.rest.records.input.EnergyBillInput;

/**
 * Calculations of refuels and charges: the missing one of total, unit and price
 * per unit, the consumption and the cost per km
 */
final class EnergyBills
{
	private EnergyBills()
	{
	}

	static BigDecimal calculateUnit(EnergyBillInput input)
	{
		BigDecimal unit = input.getUnit();
		if (!valueWasSet(unit))
		{
			BigDecimal priceInEur = input.getPricePerUnit().divide(ONE_HUNDRED, 3, RoundingMode.HALF_UP);
			unit = input.getTotal().divide(priceInEur, 2, RoundingMode.HALF_UP);
		}
		return unit;
	}

	static void calculateTotal(BillEntity entity, BigDecimal unit, EnergyBillInput input)
	{
		BigDecimal total = input.getTotal();
		if (!valueWasSet(total))
		{
			BigDecimal tmp = input.getPricePerUnit().divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);
			total = unit.multiply(tmp);
		}
		entity.setTotal(total.setScale(2, RoundingMode.HALF_UP));
	}

	/**
	 * @return price per unit in cent
	 */
	static BigDecimal calculatePricePerUnit(EnergyBillInput input, BillEntity entity, BigDecimal unit)
	{
		BigDecimal pricePerUnit = input.getPricePerUnit();
		if (!valueWasSet(pricePerUnit))
		{
			pricePerUnit = entity.getTotal()
					.multiply(ONE_HUNDRED)
					.divide(unit, 2, RoundingMode.HALF_UP);
		}
		return pricePerUnit;
	}

	static void calculateAvgConsumption(EnergyBill entity)
	{
		BigDecimal distance = entity.getDistance();
		BigDecimal unit = entity.getUnit();

		if (entity.isDistanceMissing())
		{
			return;
		}

		BigDecimal avgConsumption = unit
				.divide(distance, 6, RoundingMode.HALF_UP)
				.multiply(ONE_HUNDRED)
				.setScale(2, RoundingMode.HALF_UP);
		entity.setAvgConsumption(avgConsumption);
	}

	static void calculateCostPerKm(EnergyBill entity)
	{
		BigDecimal distance = entity.getDistance();
		BigDecimal total = entity.getBill().getTotal();

		if (entity.isDistanceMissing())
		{
			return;
		}

		BigDecimal costPerKm = total
				.divide(distance, 4, RoundingMode.HALF_UP)
				.multiply(ONE_HUNDRED).setScale(2,
						RoundingMode.HALF_UP);
		entity.setCostPerKm(costPerKm);
	}
}
