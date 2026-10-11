package de.codeflowwizardry.carledger.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.car.recurring.RecurringBill;

class RecurringBillEntityTest
{
	private static RecurringBillEntity bill(BillInterval interval, LocalDate startDate)
	{
		RecurringBillEntity entity = new RecurringBillEntity();
		entity.setInterval(interval);
		entity.setStartDate(startDate);
		entity.setNextDueDate(startDate);
		return entity;
	}

	@Test
	void shouldNotDriftAtEndOfMonth()
	{
		RecurringBillEntity entity = bill(BillInterval.MONTHLY, LocalDate.of(2025, 1, 31));

		assertEquals(LocalDate.of(2025, 2, 28), entity.advance());
		assertEquals(LocalDate.of(2025, 3, 31), entity.advance());
		assertEquals(LocalDate.of(2025, 4, 30), entity.advance());
		assertEquals(LocalDate.of(2025, 5, 31), entity.advance());
	}

	@Test
	void shouldCorrectAlreadyDriftedDueDate()
	{
		RecurringBillEntity entity = bill(BillInterval.MONTHLY, LocalDate.of(2025, 1, 31));
		entity.setNextDueDate(LocalDate.of(2025, 3, 28));

		assertEquals(LocalDate.of(2025, 4, 30), entity.advance());
	}

	@Test
	void shouldKeepLeapDayForAnnualBills()
	{
		RecurringBillEntity entity = bill(BillInterval.ANNUALLY, LocalDate.of(2024, 2, 29));

		assertEquals(LocalDate.of(2025, 2, 28), entity.advance());
		assertEquals(LocalDate.of(2026, 2, 28), entity.advance());
		assertEquals(LocalDate.of(2027, 2, 28), entity.advance());
		assertEquals(LocalDate.of(2028, 2, 29), entity.advance());
	}

	@Test
	void shouldAdvanceQuarterlyAndSemiAnnually()
	{
		RecurringBillEntity quarterly = bill(BillInterval.QUARTERLY, LocalDate.of(2025, 11, 30));
		assertEquals(LocalDate.of(2026, 2, 28), quarterly.advance());
		assertEquals(LocalDate.of(2026, 5, 30), quarterly.advance());

		RecurringBillEntity semiAnnually = bill(BillInterval.SEMI_ANNUALLY, LocalDate.of(2025, 8, 31));
		assertEquals(LocalDate.of(2026, 2, 28), semiAnnually.advance());
		assertEquals(LocalDate.of(2026, 8, 31), semiAnnually.advance());
	}

	@Test
	void shouldBeFinishedWhenNextDueDateReachesEndDate()
	{
		RecurringBillEntity entity = bill(BillInterval.MONTHLY, LocalDate.of(2025, 1, 1));
		entity.setEndDate(LocalDate.of(2025, 3, 1));

		entity.setNextDueDate(LocalDate.of(2025, 2, 1));
		assertFalse(RecurringBill.convert(entity).finished());

		entity.setNextDueDate(LocalDate.of(2025, 3, 1));
		assertTrue(RecurringBill.convert(entity).finished());
	}

	@Test
	void shouldBeFinishedAfterOncePayment()
	{
		RecurringBillEntity entity = bill(BillInterval.ONCE, LocalDate.of(2025, 1, 1));
		assertFalse(RecurringBill.convert(entity).finished());

		entity.advance();
		assertTrue(RecurringBill.convert(entity).finished());
	}
}
