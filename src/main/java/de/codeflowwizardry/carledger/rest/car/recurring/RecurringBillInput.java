package de.codeflowwizardry.carledger.rest.car.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}
