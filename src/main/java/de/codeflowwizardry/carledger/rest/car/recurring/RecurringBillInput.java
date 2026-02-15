package de.codeflowwizardry.carledger.rest.car.recurring;

import static de.codeflowwizardry.carledger.Utils.valueWasSet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.codeflowwizardry.carledger.data.BillCategory;
import de.codeflowwizardry.carledger.data.BillInterval;

public record RecurringBillInput(String name,
		String description,
		BillInterval billInterval,
		BillCategory category,
		LocalDate startDate,
		LocalDate endDate,
		BigDecimal amount)
{

	/**
	 * @return a list of errors, empty if validation succeeded
	 */
	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (StringUtils.isBlank(name))
		{
			errors.add("Name cannot be blank!");
		}
		if (category == null)
		{
			errors.add("Category must be set!");
		}
		if (billInterval == null)
		{
			errors.add("Interval must be set!");
		}
		if (startDate == null)
		{
			errors.add("Start date must be set!");
		}
		if (!valueWasSet(amount))
		{
			errors.add("Amount must be set!");
		}

		return errors;
	}
}
