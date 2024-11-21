package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillPojoBuilder
{
	private Long id;
	private LocalDate day;
	private BigDecimal distance;
	private BigDecimal unit;
	private BigDecimal pricePerUnit;
	private BigDecimal estimate;
	private BigDecimal calculated;
	private BigDecimal calculatedPrice;

	public BillPojoBuilder setId(Long id)
	{
		this.id = id;
		return this;
	}

	public BillPojoBuilder setDay(LocalDate day)
	{
		this.day = day;
		return this;
	}

	public BillPojoBuilder setDistance(BigDecimal distance)
	{
		this.distance = distance;
		return this;
	}

	public BillPojoBuilder setUnit(BigDecimal unit)
	{
		this.unit = unit;
		return this;
	}

	public BillPojoBuilder setPricePerUnit(BigDecimal pricePerUnit)
	{
		this.pricePerUnit = pricePerUnit;
		return this;
	}

	public BillPojoBuilder setEstimate(BigDecimal estimate)
	{
		this.estimate = estimate;
		return this;
	}

	public BillPojoBuilder setCalculated(BigDecimal calculated)
	{
		this.calculated = calculated;
		return this;
	}

	public BillPojoBuilder setCalculatedPrice(BigDecimal calculatedPrice)
	{
		this.calculatedPrice = calculatedPrice;
		return this;
	}

	public BillPojo createBillPojo()
	{
		return new BillPojo(id, day, distance, unit, pricePerUnit, estimate, calculated, calculatedPrice);
	}
}
