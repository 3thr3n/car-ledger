package de.codeflowwizardry.carledger;

import java.util.List;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.ModificationEntity;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.ModificationRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
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
	private final ModificationRepository modificationRepository;
	private final CarRepository carRepository;
	private final BillRepository billRepository;

	@Inject
	public ModificationService(ModificationRepository modificationRepository, CarRepository carRepository,
			BillRepository billRepository)
	{
		this.modificationRepository = modificationRepository;
		this.carRepository = carRepository;
		this.billRepository = billRepository;
	}

	/**
	 * @return the installed ones first, then the latest installed
	 */
	public List<Modification> list(long carId, String username, ModificationState state)
	{
		return modificationRepository.list(carId, username)
				.stream()
				.filter(entity -> switch (state)
				{
					case INSTALLED -> entity.isInstalled();
					case REMOVED -> !entity.isInstalled();
					case ALL -> true;
				})
				.map(ModificationService::convert)
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
		entity.setBill(bill(car, input.billId()));
		modificationRepository.persist(entity);
		return convert(entity);
	}

	@Transactional
	public Modification update(long carId, long id, ModificationInput input, String username)
	{
		Utils.requireValid(input.validate());

		ModificationEntity entity = find(carId, id, username);
		apply(entity, input);
		entity.setBill(bill(entity.getCar(), input.billId()));
		return convert(entity);
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
	 * Sets everything but the bill of a valid input
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
	 * @return the bill, an entry of the car, <code>null</code> without
	 */
	private BillEntity bill(CarEntity car, Long billId)
	{
		if (billId == null)
		{
			return null;
		}
		Optional<BillEntity> bill = billRepository.find("id = ?1 and car = ?2", billId, car).firstResultOptional();
		if (bill.isEmpty())
		{
			Utils.requireValid(List.of("The bill must be an entry of the same car!"));
		}
		return bill.get();
	}

	private static String strip(String value)
	{
		return value == null || value.isBlank() ? null : value.strip();
	}

	public static Modification convert(ModificationEntity entity)
	{
		boolean inspectionOpen = entity.isInstalled() && entity.getApprovalType().needsInspection()
				&& entity.getInspectionDate() == null;
		return new Modification(entity.getId(), entity.getCar().getId(), entity.getPart(), entity.getManufacturer(),
				entity.getApprovalType(), entity.getApprovalNumber(), entity.getConditions(),
				entity.getInspectionDate(), entity.getTestingBody(), entity.isRegistered(), entity.getInstalledDate(),
				entity.getInstalledOdometer(), entity.getRemovedDate(), entity.getRemovedOdometer(),
				entity.getNotes(), entity.getBill() == null ? null : entity.getBill().getId(), entity.isInstalled(),
				inspectionOpen);
	}
}
