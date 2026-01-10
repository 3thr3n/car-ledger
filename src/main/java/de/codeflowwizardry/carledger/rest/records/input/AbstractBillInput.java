package de.codeflowwizardry.carledger.rest.records.input;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

public abstract class AbstractBillInput
{
	private final BigDecimal total;
	private final BigInteger vatRate;
	private final LocalDate date;

	public AbstractBillInput(BigDecimal total, BigInteger vatRate, LocalDate date)
	{
		this.total = total;
		this.vatRate = vatRate;
		this.date = date;
	}

	public BigDecimal getTotal()
	{
		return total;
	}

	public BigInteger getVatRate()
	{
		return vatRate;
	}

	public LocalDate getDate()
	{
		return date;
	}
}
