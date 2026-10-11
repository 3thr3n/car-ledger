package de.codeflowwizardry.carledger.rest.car.maintenance;

import java.math.BigDecimal;
import java.time.LocalDate;

import de.codeflowwizardry.carledger.rest.records.input.AbstractBillInput;

public class MaintenanceBillInput extends AbstractBillInput
{
	private final Integer odometer;
	private final BigDecimal laborCost;
	private final BigDecimal partsCost;
	private final String description;
	private final String workshop;

	public MaintenanceBillInput(BigDecimal total, Integer vatRate, LocalDate date, Integer odometer,
			BigDecimal laborCost, BigDecimal partsCost, String description, String workshop)
	{
		super(total, vatRate, date);
		this.odometer = odometer;
		this.laborCost = laborCost;
		this.partsCost = partsCost;
		this.description = description;
		this.workshop = workshop;
	}

	public Integer getOdometer()
	{
		return odometer;
	}

	public BigDecimal getLaborCost()
	{
		return laborCost;
	}

	public BigDecimal getPartsCost()
	{
		return partsCost;
	}

	public String getDescription()
	{
		return description;
	}

	public String getWorkshop()
	{
		return workshop;
	}
}
