package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FuelBillBuilder
{
	private Long id;
	private LocalDate date;
	private BigDecimal distance;
	private BigDecimal unit;
	private BigDecimal pricePerUnit;
	private BigDecimal estimate;
	private BigDecimal calculated;
	private BigDecimal calculatedPrice;

	public FuelBillBuilder setId(Long id)
	{
		this.id = id;
		return this;
	}

	public FuelBillBuilder setDate(LocalDate date)
	{
		this.date = date;
		return this;
	}

	public FuelBillBuilder setDistance(BigDecimal distance)
	{
		this.distance = distance;
		return this;
	}

	public FuelBillBuilder setUnit(BigDecimal unit)
	{
		this.unit = unit;
		return this;
	}

	public FuelBillBuilder setPricePerUnit(BigDecimal pricePerUnit)
	{
		this.pricePerUnit = pricePerUnit;
		return this;
	}

	public FuelBillBuilder setEstimate(BigDecimal estimate)
	{
		this.estimate = estimate;
		return this;
	}

	public FuelBillBuilder setCalculated(BigDecimal calculated)
	{
		this.calculated = calculated;
		return this;
	}

	public FuelBillBuilder setCalculatedPrice(BigDecimal calculatedPrice)
	{
		this.calculatedPrice = calculatedPrice;
		return this;
	}

	public FuelBill createBillPojo()
	{
		return new FuelBill(id, date, distance, unit, pricePerUnit, estimate, calculated, calculatedPrice);
	}
}
