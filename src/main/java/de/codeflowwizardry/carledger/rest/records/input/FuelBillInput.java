package de.codeflowwizardry.carledger.rest.records.input;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

public class FuelBillInput extends AbstractBillInput
{
	private final BigDecimal distance;
	private final BigDecimal unit;
	private final BigDecimal pricePerUnit;
	private final BigDecimal estimateConsumption;

	public FuelBillInput(LocalDate date, BigDecimal total, BigInteger vatRate, BigDecimal distance, BigDecimal unit,
			BigDecimal pricePerUnit, BigDecimal estimateConsumption)
	{
		super(total, vatRate, date);
		this.distance = distance;
		this.unit = unit;
		this.pricePerUnit = pricePerUnit;
		this.estimateConsumption = estimateConsumption;
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

	public BigDecimal getEstimateConsumption()
	{
		return estimateConsumption;
	}

	@Override
	public String toString()
	{
		return "FuelBillInput{" +
				"date=" + getDate() +
				", distance=" + getDistance() +
				", unit=" + getUnit() +
				", pricePerUnit=" + getPricePerUnit() +
				", estimateConsumption=" + getEstimateConsumption() +
				", vatRate=" + getVatRate() +
				", total=" + getTotal() +
				'}';
	}
}
