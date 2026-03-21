package de.codeflowwizardry.carledger.rest.records.input;

import static de.codeflowwizardry.carledger.Utils.valueWasSet;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
		if (!valueWasSet(getTotal()))
		{
			errors.add("Total must be set!");
		}
		return errors;
	}
}
