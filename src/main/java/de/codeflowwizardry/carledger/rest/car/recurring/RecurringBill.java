package de.codeflowwizardry.carledger.rest.car.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import de.codeflowwizardry.carledger.data.BillCategory;
import de.codeflowwizardry.carledger.data.RecurringBillEntity;

public record RecurringBill(Long id, LocalDate nextDueDate, BillCategory category, LocalDate startDate,
		LocalDate endDate,
		BigDecimal amount, BigDecimal total)
{
	public static RecurringBill convert(RecurringBillEntity entity)
	{
		return new RecurringBill(
				entity.getId(),
				entity.getNextDueDate(),
				entity.getCategory(),
				entity.getStartDate(),
				entity.getEndDate(),
				entity.getAmount(),
				entity.getAmount().multiply(BigDecimal.valueOf(entity.getRecurringBillPaymentEntities().size())));
	}

	public static List<RecurringBill> convert(List<RecurringBillEntity> entities)
	{
		return entities.stream().map(RecurringBill::convert).toList();
	}
}
