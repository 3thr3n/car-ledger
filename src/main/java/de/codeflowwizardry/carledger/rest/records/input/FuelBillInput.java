package de.codeflowwizardry.carledger.rest.records.input;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

public class FuelBillInput extends AbstractBillInput
{
	private final BigDecimal distance;
	private final BigDecimal unit;
	private final BigDecimal pricePerUnit;
	private final BigDecimal estimate;

	public FuelBillInput(LocalDate date, BigDecimal total, BigInteger vatRate, BigDecimal distance, BigDecimal unit,
			BigDecimal pricePerUnit, BigDecimal estimate)
	{
		super(total, vatRate, date);
		this.distance = distance;
		this.unit = unit;
		this.pricePerUnit = pricePerUnit;
		this.estimate = estimate;
	}

	public BigDecimal getDistance()
	{
		return distance;
	}

	public BigDecimal getUnit()
	{
		return unit;
	}

	public BigDecimal getPricePerUnit()
	{
		return pricePerUnit;
	}

	public BigDecimal getEstimate()
	{
		return estimate;
	}

	@Override
	public String toString()
	{
		return "FuelBillInput{" +
				"date=" + getDate() +
				", distance=" + getDistance() +
				", unit=" + getUnit() +
				", pricePerUnit=" + getPricePerUnit() +
				", estimate=" + getEstimate() +
				", vatRate=" + getVatRate() +
				", total=" + getTotal() +
				'}';
	}
}
