package de.codeflowwizardry.carledger.rest.car.fuel;

import static de.codeflowwizardry.carledger.Utils.atLeastTwoNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.codeflowwizardry.carledger.rest.records.input.AbstractBillInput;

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
	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (getDate() == null)
		{
			errors.add("Date must be set!");
		}
		if (getVatRate() == null)
		{
			errors.add("Vat rate must be set!");
		}

		if (!atLeastTwoNotNull(getUnit(), getPricePerUnit(), getTotal()))
		{
			errors.add("At least two of 'unit', 'total' or 'pricePerUnit' must be set!");
		}
		return errors;
	}
}
