package de.codeflowwizardry.carledger.attachment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.AttachmentEntity;
import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.runtime.configuration.MemorySize;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class AttachmentService
{
	static final int MAX_FILE_NAME = 200;

	private final AttachmentRepository attachmentRepository;
	private final BillRepository billRepository;
	private final CarRepository carRepository;
	private final AttachmentStorage storage;
	private final long maxSize;
	private final int maxPerBill;

	@Inject
	public AttachmentService(AttachmentRepository attachmentRepository, BillRepository billRepository,
			CarRepository carRepository, AttachmentStorage storage,
			@ConfigProperty(name = "carledger.attachments.max-size", defaultValue = "10M") MemorySize maxSize,
			@ConfigProperty(name = "carledger.attachments.max-per-bill", defaultValue = "10") int maxPerBill)
	{
		this.attachmentRepository = attachmentRepository;
		this.billRepository = billRepository;
		this.carRepository = carRepository;
		this.storage = storage;
		this.maxSize = maxSize.asLongValue();
		this.maxPerBill = maxPerBill;
	}

	/**
	 * @throws NotFoundException
	 *             if the bill does not belong to the car of the user
	 */
	@Transactional
	public List<AttachmentEntity> listForBill(long carId, long billId, String userId)
	{
		findBill(carId, billId, userId, Action.READ);
		return attachmentRepository.listForBill(carId, billId, userId);
	}

	/**
	 * Checks size, type and the amount per bill, then saves the attachment and
	 * its content
	 *
	 * @throws NotFoundException
	 *             if the bill does not belong to the car of the user
	 * @throws jakarta.ws.rs.BadRequestException
	 *             with the reasons, if the file cannot be attached
	 */
	@Transactional
	public AttachmentEntity addToBill(long carId, long billId, String userId, String fileName, byte[] content)
	{
		BillEntity bill = findBill(carId, billId, userId, Action.WRITE);

		List<String> errors = new ArrayList<>();
		if (attachmentRepository.countForBill(billId) >= maxPerBill)
		{
			errors.add("An entry can have at most " + maxPerBill + " attachments!");
		}
		FileType type = requireValid(content, false, errors);

		return save(bill.getCar(), bill, fileName, type, content);
	}

	/**
	 * Replaces the picture of the car, only images are allowed
	 *
	 * @throws NotFoundException
	 *             if the car does not belong to the user
	 * @throws jakarta.ws.rs.BadRequestException
	 *             with the reasons, if the file cannot be used
	 */
	@Transactional
	public AttachmentEntity setCarPicture(long carId, String userId, String fileName, byte[] content)
	{
		CarEntity car = findCar(carId, userId, Action.MANAGE);
		FileType type = requireValid(content, true, new ArrayList<>());

		Long previous = car.getPictureId();
		AttachmentEntity picture = save(car, null, fileName, type, content);
		car.setPictureId(picture.getId());
		if (previous != null)
		{
			attachmentRepository.findByIdOptional(previous).ifPresent(this::remove);
		}
		return picture;
	}

	/**
	 * @throws NotFoundException
	 *             if the car does not belong to the user or has no picture
	 */
	@Transactional
	public Content loadCarPicture(long carId, String userId)
	{
		CarEntity car = findCar(carId, userId, Action.READ);
		if (car.getPictureId() == null)
		{
			throw new NotFoundException("The car has no picture");
		}
		return load(carId, car.getPictureId(), userId);
	}

	/**
	 * Removes the picture, the car shows the generated avatar again
	 *
	 * @throws NotFoundException
	 *             if the car does not belong to the user
	 */
	@Transactional
	public void removeCarPicture(long carId, String userId)
	{
		CarEntity car = findCar(carId, userId, Action.MANAGE);
		Long picture = car.getPictureId();
		car.setPictureId(null);
		if (picture != null)
		{
			attachmentRepository.findByIdOptional(picture).ifPresent(this::remove);
		}
	}

	/**
	 * @param onlyImages
	 *            whether PDFs are rejected
	 * @param errors
	 *            further errors, all are reported together
	 * @return the detected type
	 */
	private FileType requireValid(byte[] content, boolean onlyImages, List<String> errors)
	{
		Optional<FileType> type = FileType.detect(content).filter(t -> !onlyImages || t.isImage());
		if (content.length == 0)
		{
			errors.add("The file is empty!");
		} else if (content.length > maxSize)
		{
			errors.add("The file is larger than " + maxSize / 1024 / 1024 + " MB!");
		} else if (type.isEmpty())
		{
			errors.add(onlyImages
					? "Only JPEG, PNG and WebP pictures can be used!"
					: "Only JPEG, PNG, WebP and PDF files can be attached!");
		}
		Utils.requireValid(errors);
		return type.orElseThrow();
	}

	/**
	 * @param bill
	 *            <code>null</code> for a file of the car, like its picture
	 */
	private AttachmentEntity save(CarEntity car, BillEntity bill, String fileName, FileType type, byte[] content)
	{
		AttachmentEntity attachment = new AttachmentEntity();
		attachment.setCar(car);
		attachment.setBill(bill);
		attachment.setFileName(cleanFileName(fileName));
		attachment.setContentType(type.contentType());
		attachment.setSize(content.length);
		attachment.setStorage(storage.name());
		attachmentRepository.persistAndFlush(attachment);

		storage.store(attachment, content);
		return attachment;
	}

	/**
	 * Saves a file of a restored backup, checked like an upload: a picture of the
	 * car without a bill, an attachment of the bill otherwise
	 *
	 * @throws jakarta.ws.rs.BadRequestException
	 *             with the reasons, if the file cannot be used
	 */
	public AttachmentEntity restore(CarEntity car, BillEntity bill, String fileName, byte[] content)
	{
		FileType type = requireValid(content, bill == null, new ArrayList<>());
		return save(car, bill, fileName, type, content);
	}

	/**
	 * @return the content of an attachment, for the backup
	 */
	public byte[] content(AttachmentEntity attachment)
	{
		return storageOf(attachment).load(attachment);
	}

	/**
	 * @return the largest file in bytes
	 */
	public long maxSize()
	{
		return maxSize;
	}

	public int maxPerBill()
	{
		return maxPerBill;
	}

	private void remove(AttachmentEntity attachment)
	{
		storageOf(attachment).delete(attachment);
		attachmentRepository.delete(attachment);
	}

	/**
	 * @throws NotFoundException
	 *             if the attachment does not belong to the car of the user
	 */
	@Transactional
	public Content load(long carId, long id, String userId)
	{
		AttachmentEntity attachment = find(carId, id, userId, Action.READ);
		return new Content(attachment, storageOf(attachment).load(attachment));
	}

	/**
	 * @throws NotFoundException
	 *             if the attachment does not belong to the car of the user
	 */
	@Transactional
	public void delete(long carId, long id, String userId)
	{
		remove(find(carId, id, userId, Action.WRITE));
	}

	/**
	 * @return the name without path and control characters, shortened to
	 *         {@value #MAX_FILE_NAME} characters
	 */
	static String cleanFileName(String fileName)
	{
		String name = StringUtils.defaultString(fileName);
		name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
		name = name.replaceAll("\\p{Cntrl}", "").strip();
		if (name.isEmpty())
		{
			return "attachment";
		}
		return StringUtils.truncate(name, MAX_FILE_NAME);
	}

	private AttachmentEntity find(long carId, long id, String userId, Action action)
	{
		return attachmentRepository.findForUser(carId, id, userId, action)
				.orElseThrow(() -> new NotFoundException("Attachment not found"));
	}

	private BillEntity findBill(long carId, long billId, String userId, Action action)
	{
		return billRepository.find("id = :billId and car.id = :carId and " + CarAccess.condition("car", action),
				Map.of("billId", billId, "carId", carId, CarAccess.USER, userId))
				.firstResultOptional()
				.orElseThrow(() -> new NotFoundException("Bill not found"));
	}

	private CarEntity findCar(long carId, String userId, Action action)
	{
		CarEntity car = carRepository.findById(carId, userId, action);
		if (car == null)
		{
			throw new NotFoundException("Car not found");
		}
		return car;
	}

	private AttachmentStorage storageOf(AttachmentEntity attachment)
	{
		if (!storage.name().equals(attachment.getStorage()))
		{
			throw new IllegalStateException("Storage " + attachment.getStorage() + " is not configured");
		}
		return storage;
	}

	public record Content(AttachmentEntity attachment, byte[] data)
	{
	}
}
