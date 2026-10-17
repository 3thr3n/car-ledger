package de.codeflowwizardry.carledger.data.factory;

import static de.codeflowwizardry.carledger.data.BillInterval.ONCE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.RecurringBillEntity;
import de.codeflowwizardry.carledger.data.RecurringBillPaymentEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillPaymentRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.car.recurring.RecurringBillInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RecurringBillFactory
{
	private final RecurringBillRepository recurringBillRepository;
	private final RecurringBillPaymentRepository recurringBillPaymentRepository;
	private final CarRepository carRepository;

	@Inject
	public RecurringBillFactory(RecurringBillRepository recurringBillRepository,
			RecurringBillPaymentRepository recurringBillPaymentRepository, CarRepository carRepository)
	{
		this.recurringBillRepository = recurringBillRepository;
		this.recurringBillPaymentRepository = recurringBillPaymentRepository;
		this.carRepository = carRepository;
	}

	@Transactional
	public RecurringBillEntity create(long carId, RecurringBillInput input, String name)
	{
		Utils.requireValid(input.validate());

		CarEntity car = carRepository.findById(carId, name);

		if (car == null)
		{
			throw new WrongUserException();
		}

		RecurringBillEntity recurringBillEntity = new RecurringBillEntity(car);
		recurringBillEntity.setName(input.name());
		recurringBillEntity.setDescription(input.description());
		recurringBillEntity.setCategory(input.category());
		recurringBillEntity.setInterval(input.billInterval());
		recurringBillEntity.setAmount(input.amount());
		recurringBillEntity.setStartDate(input.startDate());
		recurringBillEntity.setNextDueDate(input.startDate());
		recurringBillEntity.setEndDate(input.endDate());
		recurringBillEntity.setTotal(BigDecimal.ZERO);

		recurringBillRepository.persist(recurringBillEntity);
		bookPayments(recurringBillEntity);

		return recurringBillEntity;
	}

	/**
	 * Updates the recurring bill. A changed amount, interval, start or end date
	 * books the payments again from the start date, as if the bill was created
	 * with these values. The name, description and category keep the payments.
	 *
	 * @throws WrongUserException
	 *             if the bill does not belong to the car of the user
	 */
	@Transactional
	public RecurringBillEntity update(long carId, long billId, RecurringBillInput input, String user)
	{
		RecurringBillEntity bill = recurringBillRepository.getById(carId, user, billId);
		if (bill == null)
		{
			throw new WrongUserException();
		}
		Utils.requireValid(input.validate());

		// a bill paid once ends with its payment, the end date is set when it is booked
		LocalDate endDate = input.billInterval() == ONCE ? bill.getEndDate() : input.endDate();
		boolean scheduleChanged = bill.getInterval() != input.billInterval()
				|| !bill.getStartDate().equals(input.startDate())
				|| !Objects.equals(bill.getEndDate(), endDate)
				|| bill.getAmount().compareTo(input.amount()) != 0;

		bill.setName(input.name());
		bill.setDescription(input.description());
		bill.setCategory(input.category());

		if (scheduleChanged)
		{
			bill.setInterval(input.billInterval());
			bill.setAmount(input.amount());
			bill.setStartDate(input.startDate());
			bill.setNextDueDate(input.startDate());
			bill.setEndDate(input.billInterval() == ONCE ? null : input.endDate());

			recurringBillPaymentRepository.deleteAll(bill);
			bill.getRecurringBillPaymentEntities().clear();
			bookPayments(bill);
		}

		return bill;
	}

	/**
	 * Ends the recurring bill. The end date is exclusive, payments due on or after
	 * it are removed (it can be in the past).
	 *
	 * @throws WrongUserException
	 *             if the bill does not belong to the car of the user
	 */
	@Transactional
	public RecurringBillEntity end(long carId, long billId, LocalDate endDate, String user)
	{
		RecurringBillEntity bill = recurringBillRepository.getById(carId, user, billId);
		if (bill == null)
		{
			throw new WrongUserException();
		}

		List<String> errors = new ArrayList<>();
		if (endDate == null)
		{
			errors.add("End date must be set!");
		} else if (endDate.isBefore(bill.getStartDate()))
		{
			errors.add("End date cannot be before the start date!");
		}
		Utils.requireValid(errors);

		bill.setEndDate(endDate);
		recurringBillPaymentRepository.deleteFrom(bill, endDate);
		bill.setTotal(recurringBillPaymentRepository.sumAmount(bill));

		return bill;
	}

	/**
	 * Books the payments due before today, the later ones are booked by
	 * {@link de.codeflowwizardry.carledger.ScheduledPayments}
	 */
	private void bookPayments(RecurringBillEntity bill)
	{
		List<RecurringBillPaymentEntity> payments = generateHistory(bill);

		BigDecimal total = payments
				.stream()
				.map(RecurringBillPaymentEntity::getAmount)
				.reduce(BigDecimal::add)
				.orElse(BigDecimal.ZERO);

		bill.setTotal(total);
		recurringBillPaymentRepository.persist(payments);
		bill.getRecurringBillPaymentEntities().addAll(payments);
	}

	private List<RecurringBillPaymentEntity> generateHistory(RecurringBillEntity bill)
	{
		List<RecurringBillPaymentEntity> payments = new ArrayList<>();
		LocalDate cursor = bill.getStartDate();
		LocalDate today = LocalDate.now();
		LocalDate todayOrEnd = bill.getEndDate() != null ? bill.getEndDate() : today;

		while (cursor.isBefore(todayOrEnd) && cursor.isBefore(today))
		{
			RecurringBillPaymentEntity p = new RecurringBillPaymentEntity(bill);
			p.setDueDate(cursor);
			p.setPaidDate(cursor);
			p.setAmount(bill.getAmount());
			p.setAutoGenerated(true);
			payments.add(p);

			cursor = bill.advance();

			if (bill.isOnce())
			{
				break;
			}
		}
		return payments;
	}
}
