package de.codeflowwizardry.carledger.rest.records.input;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

public class MiscellaneousBillInput extends AbstractBillInput
{
	private final String description;

	public MiscellaneousBillInput(BigDecimal total, BigInteger vatRate, LocalDate date, String description)
	{
		super(total, vatRate, date);
		this.description = description;
	}

	public String getDescription()
	{
		return description;
	}
}
