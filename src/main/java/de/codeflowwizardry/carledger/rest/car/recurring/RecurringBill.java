package de.codeflowwizardry.carledger.rest.car.recurring;

import java.math.BigDecimal;
import java.time.LocalDate;

import de.codeflowwizardry.carledger.data.BillCategory;
import de.codeflowwizardry.carledger.data.BillInterval;
import de.codeflowwizardry.carledger.data.RecurringBillEntity;

public record RecurringBill(
		Long id,
		String name,
		String description,
		LocalDate nextDueDate,
		BillCategory category,
		BillInterval billInterval,
		LocalDate startDate,
		LocalDate endDate,
		BigDecimal amount,
		BigDecimal total,
		boolean finished,
		long attachments,
		Long photo)
{
	/**
	 * @param attachments
	 *            amount of attached files, only set in the list
	 * @param photo
	 *            the first attached photo, shown small in the list
	 */
	public static RecurringBill convert(RecurringBillEntity entity, long attachments, Long photo)
	{
		RecurringBill bill = convert(entity);
		return new RecurringBill(bill.id, bill.name, bill.description, bill.nextDueDate, bill.category,
				bill.billInterval, bill.startDate, bill.endDate, bill.amount, bill.total, bill.finished, attachments,
				photo);
	}

	public static RecurringBill convert(RecurringBillEntity entity)
	{
		boolean finished = false;
		if (entity.getEndDate() != null && entity.getNextDueDate() != null)
		{
			// end date is exclusive, same as in the scheduler and the "onlyRunning" filter
			finished = !entity.getNextDueDate().isBefore(entity.getEndDate());
		}

		return new RecurringBill(
				entity.getId(),
				entity.getName(),
				entity.getDescription(),
				entity.getNextDueDate(),
				entity.getCategory(),
				entity.getInterval(),
				entity.getStartDate(),
				entity.getEndDate(),
				entity.getAmount(),
				entity.getTotal(),
				finished,
				0,
				null);
	}
}
