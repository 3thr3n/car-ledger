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
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
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
	private final AttachmentStorage storage;
	private final long maxSize;
	private final int maxPerBill;

	@Inject
	public AttachmentService(AttachmentRepository attachmentRepository, BillRepository billRepository,
			AttachmentStorage storage,
			@ConfigProperty(name = "carledger.attachments.max-size", defaultValue = "10M") MemorySize maxSize,
			@ConfigProperty(name = "carledger.attachments.max-per-bill", defaultValue = "10") int maxPerBill)
	{
		this.attachmentRepository = attachmentRepository;
		this.billRepository = billRepository;
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
		findBill(carId, billId, userId);
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
		BillEntity bill = findBill(carId, billId, userId);

		List<String> errors = new ArrayList<>();
		Optional<FileType> type = FileType.detect(content);
		if (content.length == 0)
		{
			errors.add("The file is empty!");
		} else if (content.length > maxSize)
		{
			errors.add("The file is larger than " + maxSize / 1024 / 1024 + " MB!");
		} else if (type.isEmpty())
		{
			errors.add("Only JPEG, PNG, WebP and PDF files can be attached!");
		}
		if (attachmentRepository.countForBill(billId) >= maxPerBill)
		{
			errors.add("An entry can have at most " + maxPerBill + " attachments!");
		}
		Utils.requireValid(errors);

		AttachmentEntity attachment = new AttachmentEntity();
		attachment.setCar(bill.getCar());
		attachment.setBill(bill);
		attachment.setFileName(cleanFileName(fileName));
		attachment.setContentType(type.orElseThrow().contentType());
		attachment.setSize(content.length);
		attachment.setStorage(storage.name());
		attachmentRepository.persistAndFlush(attachment);

		storage.store(attachment, content);
		return attachment;
	}

	/**
	 * @throws NotFoundException
	 *             if the attachment does not belong to the car of the user
	 */
	@Transactional
	public Content load(long carId, long id, String userId)
	{
		AttachmentEntity attachment = find(carId, id, userId);
		return new Content(attachment, storageOf(attachment).load(attachment));
	}

	/**
	 * @throws NotFoundException
	 *             if the attachment does not belong to the car of the user
	 */
	@Transactional
	public void delete(long carId, long id, String userId)
	{
		AttachmentEntity attachment = find(carId, id, userId);
		storageOf(attachment).delete(attachment);
		attachmentRepository.delete(attachment);
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

	private AttachmentEntity find(long carId, long id, String userId)
	{
		return attachmentRepository.findForUser(carId, id, userId)
				.orElseThrow(() -> new NotFoundException("Attachment not found"));
	}

	private BillEntity findBill(long carId, long billId, String userId)
	{
		return billRepository.find("id = :billId and car.id = :carId and car.user.userId = :userId",
				Map.of("billId", billId, "carId", carId, "userId", userId))
				.firstResultOptional()
				.orElseThrow(() -> new NotFoundException("Bill not found"));
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
