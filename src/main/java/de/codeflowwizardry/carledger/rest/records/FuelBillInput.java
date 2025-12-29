package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

public record FuelBillInput(LocalDate day, BigDecimal distance, BigDecimal unit, BigDecimal pricePerUnit,
		BigDecimal estimate, BigInteger vatRate, BigDecimal total)
{
	/**
	 *
	 * @param day
	 * @param unit
	 *            in liters
	 * @param pricePerUnit
	 *            in cents
	 * @param vatRate
	 *            in percent
	 */
	public FuelBillInput(LocalDate day, BigDecimal unit, BigDecimal pricePerUnit, BigInteger vatRate)
	{
		this(day, null, unit, pricePerUnit, null, vatRate, null);
	}

	@Override
	public String toString()
	{
		return "FuelBillInput{" +
				"day=" + day +
				", distance=" + distance +
				", unit=" + unit +
				", pricePerUnit=" + pricePerUnit +
				", estimate=" + estimate +
				", vatRate=" + vatRate +
				", total=" + total +
				'}';
	}
}
