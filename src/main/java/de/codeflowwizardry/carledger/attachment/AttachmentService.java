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
import de.codeflowwizardry.carledger.data.DocumentKind;
import de.codeflowwizardry.carledger.data.ModificationEntity;
import de.codeflowwizardry.carledger.data.TechnicalInspectionEntity;
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.ModificationRepository;
import de.codeflowwizardry.carledger.data.repository.TechnicalInspectionRepository;
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
	private final ModificationRepository modificationRepository;
	private final TechnicalInspectionRepository technicalInspectionRepository;
	private final AttachmentStorage storage;
	private final long maxSize;
	private final int maxPerBill;

	@Inject
	public AttachmentService(AttachmentRepository attachmentRepository, BillRepository billRepository,
			CarRepository carRepository, ModificationRepository modificationRepository,
			TechnicalInspectionRepository technicalInspectionRepository, AttachmentStorage storage,
			@ConfigProperty(name = "carledger.attachments.max-size", defaultValue = "10M") MemorySize maxSize,
			@ConfigProperty(name = "carledger.attachments.max-per-bill", defaultValue = "10") int maxPerBill)
	{
		this.attachmentRepository = attachmentRepository;
		this.billRepository = billRepository;
		this.carRepository = carRepository;
		this.modificationRepository = modificationRepository;
		this.technicalInspectionRepository = technicalInspectionRepository;
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

		return save(of(bill), fileName, type, content);
	}

	/**
	 * @throws NotFoundException
	 *             if the modification does not belong to the car of the user
	 */
	@Transactional
	public List<AttachmentEntity> listForModification(long carId, long modificationId, String userId)
	{
		findModification(carId, modificationId, userId, Action.READ);
		return attachmentRepository.listForModification(carId, modificationId, userId);
	}

	/**
	 * Checks kind, size, type and the amount per modification (the same as per
	 * bill), then saves the document and its content
	 *
	 * @throws NotFoundException
	 *             if the modification does not belong to the car of the user
	 * @throws jakarta.ws.rs.BadRequestException
	 *             with the reasons, if the file cannot be attached
	 */
	@Transactional
	public AttachmentEntity addToModification(long carId, long modificationId, String userId, String fileName,
			DocumentKind kind, byte[] content)
	{
		ModificationEntity modification = findModification(carId, modificationId, userId, Action.WRITE);
		return addDocument(modification, kind, fileName, content,
				attachmentRepository.countForModification(modificationId));
	}

	/**
	 * @throws NotFoundException
	 *             if the inspection does not belong to the car of the user
	 */
	@Transactional
	public List<AttachmentEntity> listForTechnicalInspection(long carId, long inspectionId, String userId)
	{
		findTechnicalInspection(carId, inspectionId, userId, Action.READ);
		return attachmentRepository.listForTechnicalInspection(carId, inspectionId, userId);
	}

	/**
	 * Checks size, type and the amount per inspection (the same as per bill), then
	 * saves the file, e.g. the report
	 *
	 * @throws NotFoundException
	 *             if the inspection does not belong to the car of the user
	 * @throws jakarta.ws.rs.BadRequestException
	 *             with the reasons, if the file cannot be attached
	 */
	@Transactional
	public AttachmentEntity addToTechnicalInspection(long carId, long inspectionId, String userId,
			String fileName, byte[] content)
	{
		TechnicalInspectionEntity inspection = findTechnicalInspection(carId, inspectionId, userId,
				Action.WRITE);
		return addReport(inspection, fileName, content,
				attachmentRepository.countForTechnicalInspection(inspectionId));
	}

	/**
	 * @param files
	 *            the inspection has already
	 */
	private AttachmentEntity addReport(TechnicalInspectionEntity inspection, String fileName, byte[] content,
			long files)
	{
		List<String> errors = new ArrayList<>();
		if (files >= maxPerBill)
		{
			errors.add("An inspection can have at most " + maxPerBill + " files!");
		}
		FileType type = requireValid(content, false, errors);

		AttachmentEntity report = of(inspection.getCar());
		report.setTechnicalInspection(inspection);
		return save(report, fileName, type, content);
	}

	/**
	 * @param documents
	 *            the modification has already
	 */
	private AttachmentEntity addDocument(ModificationEntity modification, DocumentKind kind, String fileName,
			byte[] content, long documents)
	{
		List<String> errors = new ArrayList<>();
		if (kind == null)
		{
			errors.add("The kind of the document must be set!");
		}
		if (documents >= maxPerBill)
		{
			errors.add("A modification can have at most " + maxPerBill + " documents!");
		}
		FileType type = requireValid(content, false, errors);

		AttachmentEntity document = of(modification.getCar());
		document.setModification(modification);
		document.setKind(kind);
		return save(document, fileName, type, content);
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
		AttachmentEntity picture = save(of(car), fileName, type, content);
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
	 * @return a file of the car, like its picture, until its owner is set
	 */
	private static AttachmentEntity of(CarEntity car)
	{
		AttachmentEntity attachment = new AttachmentEntity();
		attachment.setCar(car);
		return attachment;
	}

	private static AttachmentEntity of(BillEntity bill)
	{
		AttachmentEntity attachment = of(bill.getCar());
		attachment.setBill(bill);
		return attachment;
	}

	/**
	 * @param attachment
	 *            with the car and its owner, see {@link #of(CarEntity)}
	 */
	private AttachmentEntity save(AttachmentEntity attachment, String fileName, FileType type, byte[] content)
	{
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
		return save(bill == null ? of(car) : of(bill), fileName, type, content);
	}

	/**
	 * Saves a document of a modification of a restored backup, checked like an
	 * upload
	 *
	 * @param documents
	 *            the modification has already
	 * @throws jakarta.ws.rs.BadRequestException
	 *             with the reasons, if the file cannot be used
	 */
	public AttachmentEntity restore(ModificationEntity modification, DocumentKind kind, String fileName,
			byte[] content, int documents)
	{
		return addDocument(modification, kind, fileName, content, documents);
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

	/**
	 * Saves a file of an inspection of a restored backup, checked like an upload
	 *
	 * @param files
	 *            the inspection has already
	 */
	public AttachmentEntity restore(TechnicalInspectionEntity inspection, String fileName, byte[] content,
			int files)
	{
		return addReport(inspection, fileName, content, files);
	}

	private TechnicalInspectionEntity findTechnicalInspection(long carId, long inspectionId, String userId,
			Action action)
	{
		return technicalInspectionRepository.find(carId, inspectionId, userId, action)
				.orElseThrow(() -> new NotFoundException("Technical inspection not found"));
	}

	private ModificationEntity findModification(long carId, long modificationId, String userId, Action action)
	{
		return modificationRepository.find(carId, modificationId, userId, action)
				.orElseThrow(() -> new NotFoundException("Modification not found"));
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
