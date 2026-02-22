package de.codeflowwizardry.carledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.*;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillPaymentRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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
	ScheduledPayments scheduledPayments;

	CarEntity carEntity;

	@BeforeEach
	@Transactional
	void setup()
	{
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setMaxCars(2);
		accountEntity.setUserId("peter");
		accountRepository.persist(accountEntity);

		carEntity = new CarEntity();
		carEntity.setUser(accountEntity);
		carEntity.setName("Normal car");
		carRepository.persist(carEntity);
	}

	@Test
	@Transactional
	void shouldCreateOneEntry()
	{
		// given
		RecurringBillEntity recurringBillEntity = new RecurringBillEntity(carEntity);
		recurringBillEntity.setAmount(BigDecimal.valueOf(480));
		recurringBillEntity.setCategory(BillCategory.FINANCE);
		recurringBillEntity.setInterval(BillInterval.MONTHLY);
		recurringBillEntity.setStartDate(LocalDate.now());
		recurringBillEntity.setNextDueDate(LocalDate.now());
		recurringBillEntity.setName("Test");
		recurringBillEntity.setTotal(BigDecimal.ZERO);

		recurringBillRepository.persist(recurringBillEntity);

		// when
		scheduledPayments.generateDuePayments();

		// then
		List<RecurringBillPaymentEntity> entities = recurringBillPaymentRepository.listAll();

		Assertions.assertEquals(1, entities.size());
		Assertions.assertEquals(recurringBillEntity.getId(), entities.get(0).getBill().getId());
	}

	@Test
	@Transactional
	void shouldSkipOnceEntries()
	{
		// given
		RecurringBillEntity recurringBillEntity = new RecurringBillEntity(carEntity);
		recurringBillEntity.setAmount(BigDecimal.valueOf(480));
		recurringBillEntity.setCategory(BillCategory.FINANCE);
		recurringBillEntity.setInterval(BillInterval.ONCE);
		recurringBillEntity.setStartDate(LocalDate.now());
		recurringBillEntity.setName("Test");
		recurringBillEntity.setTotal(BigDecimal.ZERO);

		recurringBillRepository.persist(recurringBillEntity);

		// when
		scheduledPayments.generateDuePayments();

		// then
		List<RecurringBillPaymentEntity> entities = recurringBillPaymentRepository.listAll();

		Assertions.assertEquals(0, entities.size());
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		recurringBillRepository.deleteAll();
		carRepository.deleteAll();
		accountRepository.deleteAll();
	}

}
