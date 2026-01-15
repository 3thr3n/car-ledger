package de.codeflowwizardry.carledger.rest.records.input;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

public class MaintenanceBillInput extends AbstractBillInput
{
	private final BigInteger odometer;
	private final BigDecimal laborCost;
	private final BigDecimal partsCost;
	private final String description;
	private final String workshop;

	public MaintenanceBillInput(BigDecimal total, BigInteger vatRate, LocalDate date, BigInteger odometer,
			BigDecimal laborCost, BigDecimal partsCost, String description, String workshop)
	{
		super(total, vatRate, date);
		this.odometer = odometer;
		this.laborCost = laborCost;
		this.partsCost = partsCost;
		this.description = description;
		this.workshop = workshop;
	}

	public BigInteger getOdometer()
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
