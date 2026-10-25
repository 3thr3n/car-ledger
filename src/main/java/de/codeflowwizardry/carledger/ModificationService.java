package de.codeflowwizardry.carledger;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.ModificationEntity;
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.ModificationRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.car.modification.LinkedBill;
import de.codeflowwizardry.carledger.rest.car.modification.Modification;
import de.codeflowwizardry.carledger.rest.car.modification.ModificationInput;
import de.codeflowwizardry.carledger.rest.car.modification.ModificationState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

/**
 * The modifications of a car with their approvals. A removed one stays as the
 * history of the car, only deleting removes it.
 */
@ApplicationScoped
public class ModificationService
{
	/** the part, the workshop or the Abnahme, but no refuel */
	public static final Set<BillType> LINKABLE = EnumSet.of(BillType.MAINTENANCE, BillType.MISCELLANEOUS);

	private final ModificationRepository modificationRepository;
	private final CarRepository carRepository;
	private final BillRepository billRepository;
	private final AttachmentRepository attachmentRepository;

	@Inject
	public ModificationService(ModificationRepository modificationRepository, CarRepository carRepository,
			BillRepository billRepository, AttachmentRepository attachmentRepository)
	{
		this.modificationRepository = modificationRepository;
		this.carRepository = carRepository;
		this.billRepository = billRepository;
		this.attachmentRepository = attachmentRepository;
	}

	/**
	 * @return the installed ones first, then the latest installed
	 */
	public List<Modification> list(long carId, String username, ModificationState state)
	{
		List<ModificationEntity> entities = modificationRepository.list(carId, username)
				.stream()
				.filter(entity -> switch (state)
				{
					case INSTALLED -> entity.isInstalled();
					case REMOVED -> !entity.isInstalled();
					case ALL -> true;
				})
				.toList();
		Map<Long, Long> documents = attachmentRepository
				.countPerModification(entities.stream().map(ModificationEntity::getId).toList());
		return entities.stream()
				.map(entity -> convert(entity, documents.getOrDefault(entity.getId(), 0L)))
				.toList();
	}

	@Transactional
	public Modification create(long carId, ModificationInput input, String username)
	{
		Utils.requireValid(input.validate());

		CarEntity car = carRepository.findById(carId, username, Action.WRITE);
		if (car == null)
		{
			throw new WrongUserException();
		}

		ModificationEntity entity = new ModificationEntity(car);
		apply(entity, input);
		entity.setBills(bills(car, input.billIds()));
		modificationRepository.persist(entity);
		return convert(entity, 0);
	}

	@Transactional
	public Modification update(long carId, long id, ModificationInput input, String username)
	{
		Utils.requireValid(input.validate());

		ModificationEntity entity = find(carId, id, username);
		apply(entity, input);
		entity.setBills(bills(entity.getCar(), input.billIds()));
		return convert(entity, attachmentRepository.countForModification(id));
	}

	@Transactional
	public void delete(long carId, long id, String username)
	{
		modificationRepository.delete(find(carId, id, username));
	}

	private ModificationEntity find(long carId, long id, String username)
	{
		return modificationRepository.find(carId, id, username, Action.WRITE)
				.orElseThrow(() -> new NotFoundException("Modification not found"));
	}

	/**
	 * @return the bills that can be linked to a modification, the latest first
	 */
	public List<LinkedBill> linkableBills(long carId, String username)
	{
		CarEntity car = carRepository.findById(carId, username, Action.READ);
		if (car == null)
		{
			throw new WrongUserException();
		}
		return billRepository.getBills(carId, username, Optional.empty(), Optional.empty())
				.stream()
				.filter(bill -> LINKABLE.contains(bill.getType()))
				.map(LinkedBill::convert)
				.toList();
	}

	/**
	 * Sets everything but the bills of a valid input
	 */
	public static void apply(ModificationEntity entity, ModificationInput input)
	{
		entity.setPart(input.part().strip());
		entity.setManufacturer(strip(input.manufacturer()));
		entity.setApprovalType(input.approvalType());
		entity.setApprovalNumber(strip(input.approvalNumber()));
		entity.setConditions(strip(input.conditions()));
		entity.setInspectionDate(input.inspectionDate());
		entity.setTestingBody(strip(input.testingBody()));
		entity.setRegistered(input.registered());
		entity.setInstalledDate(input.installedDate());
		entity.setInstalledOdometer(input.installedOdometer());
		entity.setRemovedDate(input.removedDate());
		entity.setRemovedOdometer(input.removedOdometer());
		entity.setNotes(strip(input.notes()));
	}

	/**
	 * @return the bills, maintenance and other entries of the car
	 */
	private List<BillEntity> bills(CarEntity car, List<Long> billIds)
	{
		if (billIds == null || billIds.isEmpty())
		{
			return List.of();
		}
		Set<Long> ids = new HashSet<>(billIds);
		List<BillEntity> bills = billRepository.list("id in ?1 and car = ?2 and type in ?3", ids, car, LINKABLE);
		if (bills.size() != ids.size())
		{
			Utils.requireValid(List.of("The bills must be maintenance or other entries of the same car!"));
		}
		return bills;
	}

	private static String strip(String value)
	{
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static Modification convert(ModificationEntity entity, long documents)
	{
		boolean inspectionOpen = entity.isInstalled() && entity.getApprovalType().needsInspection()
				&& entity.getInspectionDate() == null;
		return new Modification(entity.getId(), entity.getCar().getId(), entity.getPart(), entity.getManufacturer(),
				entity.getApprovalType(), entity.getApprovalNumber(), entity.getConditions(),
				entity.getInspectionDate(), entity.getTestingBody(), entity.isRegistered(), entity.getInstalledDate(),
				entity.getInstalledOdometer(), entity.getRemovedDate(), entity.getRemovedOdometer(),
				entity.getNotes(),
				entity.getBills().stream()
						.sorted(Comparator.comparing(BillEntity::getDate).thenComparing(BillEntity::getId))
						.map(LinkedBill::convert)
						.toList(),
				entity.isInstalled(),
				inspectionOpen, documents);
	}
}
