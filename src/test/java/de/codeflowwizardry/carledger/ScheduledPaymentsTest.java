package de.codeflowwizardry.carledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.*;
import de.codeflowwizardry.carledger.data.factory.RecurringBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillPaymentRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillRepository;
import de.codeflowwizardry.carledger.rest.car.recurring.RecurringBillInput;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * The job uses its own transactions, so all data is committed before and read
 * again after running it.
 */
@QuarkusTest
class ScheduledPaymentsTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	RecurringBillRepository recurringBillRepository;

	@Inject
	RecurringBillPaymentRepository recurringBillPaymentRepository;

	@Inject
	RecurringBillFactory recurringBillFactory;

	@Inject
	ScheduledPayments scheduledPayments;

	CarEntity carEntity;

	@BeforeEach
	void setup()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			AccountEntity accountEntity = new AccountEntity();
			accountEntity.setMaxCars(2);
			accountEntity.setUserId("peter");
			accountRepository.persist(accountEntity);

			carEntity = new CarEntity();
			carEntity.setUser(accountEntity);
			carEntity.setName("Normal car");
			carEntity.setManufactureYear(2020);
			carRepository.persist(carEntity);
		});
	}

	@AfterEach
	void cleanup()
	{
		QuarkusTransaction.requiringNew().run(() -> accountRepository.deleteAll());
	}

	private long persist(BillInterval interval, LocalDate startDate, LocalDate nextDueDate, LocalDate endDate,
			BigDecimal total)
	{
		return QuarkusTransaction.requiringNew().call(() -> {
			RecurringBillEntity recurringBillEntity = new RecurringBillEntity(carRepository.findById(carEntity.getId(),
					"peter", Action.WRITE));
			recurringBillEntity.setAmount(BigDecimal.valueOf(480));
			recurringBillEntity.setCategory(BillCategory.FINANCE);
			recurringBillEntity.setInterval(interval);
			recurringBillEntity.setStartDate(startDate);
			recurringBillEntity.setNextDueDate(nextDueDate);
			recurringBillEntity.setEndDate(endDate);
			recurringBillEntity.setName("Test");
			recurringBillEntity.setTotal(total);
			recurringBillRepository.persist(recurringBillEntity);
			return recurringBillEntity.getId();
		});
	}

	private List<RecurringBillPaymentEntity> payments()
	{
		return QuarkusTransaction.requiringNew()
				.call(() -> recurringBillPaymentRepository.list("order by dueDate"));
	}

	private RecurringBillEntity bill(long id)
	{
		return QuarkusTransaction.requiringNew().call(() -> recurringBillRepository.findById(id));
	}

	@Test
	void shouldCreateOneEntry()
	{
		// given
		LocalDate today = LocalDate.now();
		long id = persist(BillInterval.MONTHLY, today.minusMonths(1), today, null, BigDecimal.ZERO);

		// when
		scheduledPayments.generateDuePayments();

		// then
		List<RecurringBillPaymentEntity> entities = payments();

		Assertions.assertEquals(1, entities.size());
		Assertions.assertEquals(today, entities.get(0).getDueDate());
		Assertions.assertEquals(today.minusMonths(1).plusMonths(2), bill(id).getNextDueDate());
		Assertions.assertEquals(0, BigDecimal.valueOf(480).compareTo(bill(id).getTotal()));
	}

	@Test
	void shouldCatchUpMissedPayments()
	{
		// given: the job did not run for three months
		LocalDate startDate = LocalDate.now().minusMonths(3);
		long id = persist(BillInterval.MONTHLY, startDate, startDate, null, BigDecimal.ZERO);

		// when
		scheduledPayments.generateDuePayments();

		// then
		List<RecurringBillPaymentEntity> entities = payments();

		Assertions.assertEquals(4, entities.size());
		Assertions.assertEquals(startDate, entities.get(0).getDueDate());
		Assertions.assertEquals(startDate.plusMonths(3), entities.get(3).getDueDate());
		Assertions.assertEquals(startDate.plusMonths(4), bill(id).getNextDueDate());
		Assertions.assertEquals(0, BigDecimal.valueOf(4 * 480).compareTo(bill(id).getTotal()));
	}

	@Test
	void shouldCreateNotAfter()
	{
		// given
		persist(BillInterval.MONTHLY, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2025, 12, 31),
				BigDecimal.ZERO);

		// when
		scheduledPayments.generateDuePayments();

		// then
		Assertions.assertEquals(0, payments().size());
	}

	@Test
	void shouldStopAtEndDate()
	{
		// given
		LocalDate startDate = LocalDate.now().minusMonths(5);
		persist(BillInterval.MONTHLY, startDate, startDate, startDate.plusMonths(2), BigDecimal.ZERO);

		// when
		scheduledPayments.generateDuePayments();

		// then: end date is exclusive
		Assertions.assertEquals(2, payments().size());
	}

	@Test
	void shouldPayOnceEntryOnce()
	{
		// given
		long id = persist(BillInterval.ONCE, LocalDate.now(), LocalDate.now(), null, BigDecimal.ZERO);

		// when
		scheduledPayments.generateDuePayments();
		scheduledPayments.generateDuePayments();

		// then
		Assertions.assertEquals(1, payments().size());
		Assertions.assertEquals(LocalDate.now(), bill(id).getEndDate());
	}

	@Test
	void shouldNotPayPastOnceEntryTwice()
	{
		// given
		RecurringBillInput input = new RecurringBillInput("Test", null, BillInterval.ONCE, BillCategory.FINANCE,
				LocalDate.now().minusMonths(1), null, BigDecimal.valueOf(480));
		long id = recurringBillFactory.create(carEntity.getId(), input, "peter").getId();

		// when
		scheduledPayments.generateDuePayments();

		// then
		Assertions.assertEquals(1, payments().size());
		RecurringBillEntity recurringBillEntity = bill(id);
		Assertions.assertEquals(0, BigDecimal.valueOf(480).compareTo(recurringBillEntity.getTotal()));
		Assertions.assertEquals(recurringBillEntity.getStartDate(), recurringBillEntity.getEndDate());
	}

	@Test
	void shouldSkipAlreadyExistingPayment()
	{
		// given: a payment exists, but the bill was not advanced
		LocalDate startDate = LocalDate.now().minusMonths(1);
		long id = persist(BillInterval.ONCE, startDate, startDate, null, BigDecimal.valueOf(480));

		QuarkusTransaction.requiringNew().run(() -> {
			RecurringBillPaymentEntity payment = new RecurringBillPaymentEntity(recurringBillRepository.findById(id));
			payment.setAmount(BigDecimal.valueOf(480));
			payment.setDueDate(startDate);
			payment.setPaidDate(startDate);
			recurringBillPaymentRepository.persist(payment);
		});

		// when
		scheduledPayments.generateDuePayments();

		// then
		Assertions.assertEquals(1, payments().size());
		RecurringBillEntity recurringBillEntity = bill(id);
		Assertions.assertEquals(0, BigDecimal.valueOf(480).compareTo(recurringBillEntity.getTotal()));
		Assertions.assertEquals(startDate, recurringBillEntity.getEndDate());
	}

	@Test
	void shouldNotLetOneFailingBillStopTheOthers()
	{
		// given: the first bill cannot be paid (amount violates the not null
		// constraint)
		LocalDate today = LocalDate.now();
		long broken = persist(BillInterval.MONTHLY, today, today, null, BigDecimal.ZERO);
		QuarkusTransaction.requiringNew().run(() -> recurringBillRepository.getEntityManager()
				.createNativeQuery("alter table recurring_bill alter column r_amount drop not null")
				.executeUpdate());
		QuarkusTransaction.requiringNew().run(() -> recurringBillRepository.update("amount = null where id = ?1",
				broken));
		long working = persist(BillInterval.MONTHLY, today.minusDays(1), today.minusDays(1), null, BigDecimal.ZERO);

		try
		{
			// when
			scheduledPayments.generateDuePayments();

			// then
			List<RecurringBillPaymentEntity> entities = payments();
			Assertions.assertEquals(1, entities.size());
			Assertions.assertEquals(working, entities.get(0).getBill().getId());
		} finally
		{
			QuarkusTransaction.requiringNew().run(() -> {
				recurringBillRepository.delete("id", broken);
				recurringBillRepository.getEntityManager()
						.createNativeQuery("alter table recurring_bill alter column r_amount set not null")
						.executeUpdate();
			});
		}
	}
}
