package de.codeflowwizardry.carledger;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.ReminderEntity;
import de.codeflowwizardry.carledger.data.ReminderKind;
import de.codeflowwizardry.carledger.data.TechnicalInspectionEntity;
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.ReminderRepository;
import de.codeflowwizardry.carledger.data.repository.TechnicalInspectionRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.car.inspection.TechnicalInspection;
import de.codeflowwizardry.carledger.rest.car.inspection.TechnicalInspectionInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

/**
 * The technical inspections (HU) of a car. The next due month of the latest one
 * moves the reminder of the HU of the car.
 */
@ApplicationScoped
public class TechnicalInspectionService
{
	/** the title of the reminder, the same in all languages */
	static final String REMINDER_TITLE = "HU";

	private final TechnicalInspectionRepository inspectionRepository;
	private final CarRepository carRepository;
	private final ReminderRepository reminderRepository;
	private final AttachmentRepository attachmentRepository;

	@Inject
	public TechnicalInspectionService(TechnicalInspectionRepository inspectionRepository,
			CarRepository carRepository, ReminderRepository reminderRepository,
			AttachmentRepository attachmentRepository)
	{
		this.inspectionRepository = inspectionRepository;
		this.carRepository = carRepository;
		this.reminderRepository = reminderRepository;
		this.attachmentRepository = attachmentRepository;
	}

	/**
	 * @return the latest first
	 */
	public List<TechnicalInspection> list(long carId, String username)
	{
		List<TechnicalInspectionEntity> entities = inspectionRepository.list(carId, username);
		Map<Long, Long> documents = attachmentRepository
				.countPerTechnicalInspection(entities.stream().map(TechnicalInspectionEntity::getId).toList());
		return entities.stream()
				.map(entity -> convert(entity, documents.getOrDefault(entity.getId(), 0L)))
				.toList();
	}

	@Transactional
	public TechnicalInspection create(long carId, TechnicalInspectionInput input, String username)
	{
		Utils.requireValid(input.validate());

		CarEntity car = carRepository.findById(carId, username, Action.WRITE);
		if (car == null)
		{
			throw new WrongUserException();
		}

		TechnicalInspectionEntity entity = new TechnicalInspectionEntity(car);
		apply(entity, input);
		inspectionRepository.persist(entity);
		followReminder(car);
		return convert(entity, 0);
	}

	@Transactional
	public TechnicalInspection update(long carId, long id, TechnicalInspectionInput input, String username)
	{
		Utils.requireValid(input.validate());

		TechnicalInspectionEntity entity = find(carId, id, username);
		apply(entity, input);
		followReminder(entity.getCar());
		return convert(entity, attachmentRepository.countForTechnicalInspection(id));
	}

	/**
	 * The reminder stays as it is, the owner decides about it
	 */
	@Transactional
	public void delete(long carId, long id, String username)
	{
		inspectionRepository.delete(find(carId, id, username));
	}

	private TechnicalInspectionEntity find(long carId, long id, String username)
	{
		return inspectionRepository.find(carId, id, username, Action.WRITE)
				.orElseThrow(() -> new NotFoundException("Technical inspection not found"));
	}

	/**
	 * Sets everything of a valid input
	 */
	public static void apply(TechnicalInspectionEntity entity, TechnicalInspectionInput input)
	{
		entity.setDate(input.date());
		entity.setOdometer(input.odometer());
		entity.setTestingBody(strip(input.testingBody()));
		entity.setResult(input.result());
		entity.setDefects(strip(input.defects()));
		entity.setNextDue(input.nextDue() == null ? null : input.nextDue().withDayOfMonth(1));
	}

	/**
	 * Moves the reminder of the HU to the next due month of the latest inspection,
	 * it is due at the end of the month. One deleted by the owner comes back.
	 */
	public void followReminder(CarEntity car)
	{
		inspectionRepository.flush();
		Optional<TechnicalInspectionEntity> latest = inspectionRepository.latest(car.getId());
		if (latest.isEmpty() || latest.get().getNextDue() == null)
		{
			return;
		}
		LocalDate due = YearMonth.from(latest.get().getNextDue()).atEndOfMonth();
		Optional<ReminderEntity> reminder = reminderRepository
				.find("car = ?1 and kind = ?2", car, ReminderKind.TECHNICAL_INSPECTION)
				.firstResultOptional();
		if (reminder.isPresent())
		{
			reminder.get().setDueDate(due);
			return;
		}
		// due before it is inserted, a reminder needs a due date or odometer
		ReminderEntity created = new ReminderEntity(car);
		created.setKind(ReminderKind.TECHNICAL_INSPECTION);
		created.setTitle(REMINDER_TITLE);
		created.setDueDate(due);
		reminderRepository.persist(created);
	}

	private static String strip(String value)
	{
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static TechnicalInspection convert(TechnicalInspectionEntity entity, long documents)
	{
		return new TechnicalInspection(entity.getId(), entity.getCar().getId(), entity.getDate(),
				entity.getOdometer(), entity.getTestingBody(), entity.getResult(), entity.getDefects(),
				entity.getNextDue(), entity.getResult().passed(), documents);
	}
}
