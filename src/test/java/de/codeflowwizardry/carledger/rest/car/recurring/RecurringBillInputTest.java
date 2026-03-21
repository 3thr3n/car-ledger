package de.codeflowwizardry.carledger.rest.car.recurring;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.BillCategory;
import de.codeflowwizardry.carledger.data.BillInterval;

class RecurringBillInputTest
{
	@Test
	void shouldValidate()
	{
		// when date not set
		RecurringBillInput input = new RecurringBillInput(null, null, BillInterval.ONCE, BillCategory.FINANCE,
				LocalDate.now(), null, BigDecimal.TEN);
		List<String> validate = input.validate();
		assertLinesMatch(List.of("Name cannot be blank!"), validate);

		// when vat not set
		input = new RecurringBillInput("Bane", null, BillInterval.ONCE, null, LocalDate.now(), null, BigDecimal.TEN);
		validate = input.validate();
		assertLinesMatch(List.of("Category must be set!"), validate);

		// when total not set
		input = new RecurringBillInput("Bane", null, null, BillCategory.FINANCE, LocalDate.now(), null, BigDecimal.TEN);
		validate = input.validate();
		assertLinesMatch(List.of("Interval must be set!"), validate);

		input = new RecurringBillInput("Bane", null, BillInterval.ONCE, BillCategory.FINANCE, null, null,
				BigDecimal.TEN);
		validate = input.validate();
		assertLinesMatch(List.of("Start date must be set!"), validate);

		input = new RecurringBillInput("Bane", null, BillInterval.ONCE, BillCategory.FINANCE, LocalDate.now(), null,
				BigDecimal.ZERO);
		validate = input.validate();
		assertLinesMatch(List.of("Amount must be set!"), validate);
	}
}
