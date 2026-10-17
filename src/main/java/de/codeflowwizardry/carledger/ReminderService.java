package de.codeflowwizardry.carledger;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.ReminderEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.ReminderRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.car.reminder.Reminder;
import de.codeflowwizardry.carledger.rest.car.reminder.ReminderDoneInput;
import de.codeflowwizardry.carledger.rest.car.reminder.ReminderInput;
import de.codeflowwizardry.carledger.rest.car.reminder.ReminderStatus;
import de.codeflowwizardry.carledger.rest.records.Odometer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ReminderService
{
	/** a reminder is due soon this many days before its date */
	public static final int SOON_DAYS = 30;
	/** a reminder is due soon this many km before its odometer */
	public static final int SOON_KM = 1000;

	/** overdue first, then the ones due soon, the nearest first */
	public static final Comparator<Reminder> BY_URGENCY = Comparator
			.comparing(Reminder::status)
			.thenComparing(Reminder::daysLeft, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(Reminder::kmLeft, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(Reminder::id);

	private final ReminderRepository reminderRepository;
	private final CarRepository carRepository;
	private final OdometerService odometerService;

	@Inject
	public ReminderService(ReminderRepository reminderRepository, CarRepository carRepository,
			OdometerService odometerService)
	{
		this.reminderRepository = reminderRepository;
		this.carRepository = carRepository;
		this.odometerService = odometerService;
	}

	/**
	 * @param carId
	 *            or {@link CarRepository#ALL_CARS}
	 * @param onlyDue
	 *            only the overdue ones and the ones due soon
	 * @return the most urgent first
	 */
	public List<Reminder> list(long carId, String username, boolean onlyDue)
	{
		Map<Long, Odometer> odometers = odometerService.current(carId, username);
		LocalDate today = LocalDate.now();

		return reminderRepository.list(carId, username)
				.stream()
				.map(entity -> convert(entity, odometers.get(entity.getCar().getId()).value(), today))
				.filter(reminder -> !onlyDue || reminder.status() != ReminderStatus.UPCOMING)
				.sorted(BY_URGENCY)
				.toList();
	}

	@Transactional
	public Reminder create(long carId, ReminderInput input, String username)
	{
		Utils.requireValid(input.validate());

		CarEntity car = carRepository.findById(carId, username);
		if (car == null)
		{
			throw new WrongUserException();
		}

		ReminderEntity entity = new ReminderEntity(car);
		apply(entity, input);
		reminderRepository.persist(entity);
		return convert(entity, username);
	}

	@Transactional
	public Reminder update(long carId, long id, ReminderInput input, String username)
	{
		Utils.requireValid(input.validate());

		ReminderEntity entity = find(carId, id, username);
		apply(entity, input);
		return convert(entity, username);
	}

	@Transactional
	public void delete(long carId, long id, String username)
	{
		reminderRepository.delete(find(carId, id, username));
	}

	/**
	 * A repeating reminder is due again one interval after it was done, a due date
	 * or odometer without an interval is removed. A reminder without any interval
	 * is deleted.
	 *
	 * @return the next one, empty if it was deleted
	 */
	@Transactional
	public Optional<Reminder> done(long carId, long id, ReminderDoneInput input, String username)
	{
		ReminderEntity entity = find(carId, id, username);
		if (!entity.isRepeating())
		{
			reminderRepository.delete(entity);
			return Optional.empty();
		}

		LocalDate date = input == null || input.date() == null ? LocalDate.now() : input.date();
		Integer odometer = input == null ? null : input.odometer();
		if (odometer != null && (odometer < 0 || odometer > ReminderInput.MAX_ODOMETER))
		{
			Utils.requireValid(List.of("Odometer must be between 0 and " + ReminderInput.MAX_ODOMETER + "!"));
		}
		if (odometer == null)
		{
			odometer = currentOdometer(entity, username);
		}

		entity.setDueDate(entity.getIntervalMonths() == null ? null : date.plusMonths(entity.getIntervalMonths()));
		entity.setDueOdometer(entity.getIntervalKm() == null ? null : odometer + entity.getIntervalKm());
		return Optional.of(convert(entity, username));
	}

	private ReminderEntity find(long carId, long id, String username)
	{
		return reminderRepository.find(carId, id, username)
				.orElseThrow(() -> new NotFoundException("Reminder not found"));
	}

	private static void apply(ReminderEntity entity, ReminderInput input)
	{
		entity.setTitle(input.title().strip());
		entity.setNote(input.note() == null || input.note().isBlank() ? null : input.note().strip());
		entity.setDueDate(input.dueDate());
		entity.setDueOdometer(input.dueOdometer());
		entity.setIntervalMonths(input.intervalMonths());
		entity.setIntervalKm(input.intervalKm());
	}

	private int currentOdometer(ReminderEntity entity, String username)
	{
		long carId = entity.getCar().getId();
		return odometerService.current(carId, username).get(carId).value();
	}

	private Reminder convert(ReminderEntity entity, String username)
	{
		return convert(entity, currentOdometer(entity, username), LocalDate.now());
	}

	public static Reminder convert(ReminderEntity entity, int odometer, LocalDate today)
	{
		Long daysLeft = entity.getDueDate() == null ? null : ChronoUnit.DAYS.between(today, entity.getDueDate());
		Integer kmLeft = entity.getDueOdometer() == null ? null : entity.getDueOdometer() - odometer;

		ReminderStatus status;
		if ((daysLeft != null && daysLeft < 0) || (kmLeft != null && kmLeft <= 0))
		{
			status = ReminderStatus.OVERDUE;
		} else if ((daysLeft != null && daysLeft <= SOON_DAYS) || (kmLeft != null && kmLeft <= SOON_KM))
		{
			status = ReminderStatus.DUE_SOON;
		} else
		{
			status = ReminderStatus.UPCOMING;
		}

		CarEntity car = entity.getCar();
		return new Reminder(entity.getId(), car.getId(), car.getName(), entity.getTitle(), entity.getNote(),
				entity.getDueDate(), entity.getDueOdometer(), entity.getIntervalMonths(), entity.getIntervalKm(),
				status, odometer, daysLeft, kmLeft);
	}
}
