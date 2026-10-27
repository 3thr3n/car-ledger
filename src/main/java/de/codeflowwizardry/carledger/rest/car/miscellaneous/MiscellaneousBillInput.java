package de.codeflowwizardry.carledger.rest.car.miscellaneous;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.rest.records.input.AbstractBillInput;

public class MiscellaneousBillInput extends AbstractBillInput
{
	private final String description;

	public MiscellaneousBillInput(BigDecimal total, Integer vatRate, LocalDate date, String description)
	{
		super(total, vatRate, date);
		this.description = description;
	}

	public String getDescription()
	{
		return description;
	}

	@Override
	public List<String> validate()
	{
		List<String> errors = super.validate();
		Utils.checkLength(errors, "Description", description, 1000);
		return errors;
	}
}
