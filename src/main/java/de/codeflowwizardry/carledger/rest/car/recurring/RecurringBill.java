package de.codeflowwizardry.carledger.rest.car.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import de.codeflowwizardry.carledger.data.BillCategory;
import de.codeflowwizardry.carledger.data.RecurringBillEntity;

public record RecurringBill(
		Long id,
		String name,
		String description,
		LocalDate nextDueDate,
		BillCategory category,
		LocalDate startDate,
		LocalDate endDate,
		BigDecimal amount,
		BigDecimal total,
		boolean finished)
{
	public static RecurringBill convert(RecurringBillEntity entity)
	{
		boolean finished = false;
		if (entity.getEndDate() != null && entity.getNextDueDate() != null)
		{
			finished = entity.getEndDate().isBefore(entity.getNextDueDate());
		}

		return new RecurringBill(
				entity.getId(),
				entity.getName(),
				entity.getDescription(),
				entity.getNextDueDate(),
				entity.getCategory(),
				entity.getStartDate(),
				entity.getEndDate(),
				entity.getAmount(),
				entity.getTotal(),
				finished);
	}

	public static List<RecurringBill> convert(List<RecurringBillEntity> entities)
	{
		return entities.stream().map(RecurringBill::convert).toList();
	}
}
