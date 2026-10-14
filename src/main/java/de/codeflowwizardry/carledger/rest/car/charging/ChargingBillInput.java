package de.codeflowwizardry.carledger.rest.car.charging;

import static de.codeflowwizardry.carledger.Utils.atLeastTwoValuesSet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.codeflowwizardry.carledger.data.ChargingLocation;
import de.codeflowwizardry.carledger.rest.records.input.AbstractBillInput;
import de.codeflowwizardry.carledger.rest.records.input.EnergyBillInput;

/**
 * A charge, two of total, kWh and price per kWh are enough
 */
public class ChargingBillInput extends AbstractBillInput implements EnergyBillInput
{
	private final BigDecimal distance;
	private final BigDecimal unit;
	private final BigDecimal pricePerUnit;
	private final ChargingLocation location;
	private final Integer stateOfCharge;

	/**
	 * @param distance
	 *            optional, km since the last charge
	 * @param unit
	 *            kWh
	 * @param pricePerUnit
	 *            cent per kWh
	 * @param location
	 *            optional
	 * @param stateOfCharge
	 *            optional, percent after charging
	 */
	public ChargingBillInput(LocalDate date, BigDecimal total, Integer vatRate, BigDecimal distance,
			BigDecimal unit, BigDecimal pricePerUnit, ChargingLocation location, Integer stateOfCharge)
	{
		super(total, vatRate, date);
		this.distance = distance;
		this.unit = unit;
		this.pricePerUnit = pricePerUnit;
		this.location = location;
		this.stateOfCharge = stateOfCharge;
	}

	@Override
	public BigDecimal getDistance()
	{
		return distance;
	}

	@Override
	public BigDecimal getUnit()
	{
		return unit;
	}

	@Override
	public BigDecimal getPricePerUnit()
	{
		return pricePerUnit;
	}

	public ChargingLocation getLocation()
	{
		return location;
	}

	public Integer getStateOfCharge()
	{
		return stateOfCharge;
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
		if (!atLeastTwoValuesSet(getUnit(), getPricePerUnit(), getTotal()))
		{
			errors.add("At least two of 'unit', 'total' or 'pricePerUnit' must be set!");
		}
		if (stateOfCharge != null && (stateOfCharge < 0 || stateOfCharge > 100))
		{
			errors.add("The state of charge must be between 0 and 100 percent!");
		}
		return errors;
	}
}
