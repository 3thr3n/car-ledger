package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillBuilder
{
	private Long id;
	private LocalDate day;
	private BigDecimal distance;
	private BigDecimal unit;
	private BigDecimal pricePerUnit;
	private BigDecimal estimate;
	private BigDecimal calculated;
	private BigDecimal calculatedPrice;

	public BillBuilder setId(Long id)
	{
		this.id = id;
		return this;
	}

	public BillBuilder setDay(LocalDate day)
	{
		this.day = day;
		return this;
	}

	public BillBuilder setDistance(BigDecimal distance)
	{
		this.distance = distance;
		return this;
	}

	public BillBuilder setUnit(BigDecimal unit)
	{
		this.unit = unit;
		return this;
	}

	public BillBuilder setPricePerUnit(BigDecimal pricePerUnit)
	{
		this.pricePerUnit = pricePerUnit;
		return this;
	}

	public BillBuilder setEstimate(BigDecimal estimate)
	{
		this.estimate = estimate;
		return this;
	}

	public BillBuilder setCalculated(BigDecimal calculated)
	{
		this.calculated = calculated;
		return this;
	}

	public BillBuilder setCalculatedPrice(BigDecimal calculatedPrice)
	{
		this.calculatedPrice = calculatedPrice;
		return this;
	}

	public Bill createBillPojo()
	{
		return new Bill(id, day, distance, unit, pricePerUnit, estimate, calculated, calculatedPrice);
	}
}
