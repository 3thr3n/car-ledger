package de.codeflowwizardry.carledger.papers;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import de.codeflowwizardry.carledger.attachment.AttachmentService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.AttachmentEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.TechnicalInspectionEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.ModificationRepository;
import de.codeflowwizardry.carledger.data.repository.TechnicalInspectionRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Collects the modifications and the HUs of a car with their files for the PDF
 * of its papers, e.g. for a roadside check or when selling it
 */
@ApplicationScoped
public class CarPapersService
{
	static final List<String> LANGUAGES = List.of("de", "en");

	private final CarRepository carRepository;
	private final AccountRepository accountRepository;
	private final ModificationRepository modificationRepository;
	private final TechnicalInspectionRepository inspectionRepository;
	private final AttachmentRepository attachmentRepository;
	private final AttachmentService attachmentService;

	@Inject
	public CarPapersService(CarRepository carRepository, AccountRepository accountRepository,
			ModificationRepository modificationRepository, TechnicalInspectionRepository inspectionRepository,
			AttachmentRepository attachmentRepository, AttachmentService attachmentService)
	{
		this.carRepository = carRepository;
		this.accountRepository = accountRepository;
		this.modificationRepository = modificationRepository;
		this.inspectionRepository = inspectionRepository;
		this.attachmentRepository = attachmentRepository;
		this.attachmentService = attachmentService;
	}

	/**
	 * @param history
	 *            with the removed parts and all HUs, otherwise the installed parts
	 *            and the latest HU
	 * @param language
	 *            <code>de</code> or <code>en</code>, the one of the account
	 *            without
	 */
	public CarPapers papers(long carId, String username, boolean history, Optional<String> language)
	{
		if (language.isPresent() && !LANGUAGES.contains(language.get().toLowerCase(Locale.ROOT)))
		{
			// as JSON like everywhere, the endpoint produces PDF
			throw new BadRequestException(Response.status(400).type(MediaType.APPLICATION_JSON_TYPE)
					.entity(List.of("Language must be one of " + LANGUAGES + "!")).build());
		}
		CarEntity car = carRepository.findById(carId, username, Action.READ);
		if (car == null)
		{
			throw new WrongUserException();
		}

		List<CarPapers.Part> parts = modificationRepository.list(carId, username).stream()
				.filter(modification -> history || modification.isInstalled())
				.map(modification -> new CarPapers.Part(modification,
						files(attachmentRepository.listForModification(carId, modification.getId(), username))))
				.toList();
		List<TechnicalInspectionEntity> inspections = inspectionRepository.list(carId, username);
		List<CarPapers.Inspection> hus = inspections.stream()
				.limit(history ? inspections.size() : 1)
				.map(inspection -> new CarPapers.Inspection(inspection,
						files(attachmentRepository.listForTechnicalInspection(carId, inspection.getId(), username))))
				.toList();

		AccountEntity account = accountRepository.findByIdentifier(username);
		String lang = language.orElse(account.getMailLanguage()).toLowerCase(Locale.ROOT);
		return new CarPapers(car.getName(), car.getLicensePlate(), car.getVin(), LocalDate.now(), history, parts,
				hus, account.getUnitSystem(), LANGUAGES.contains(lang) ? lang : "en");
	}

	/**
	 * @return the PDF of {@link #papers}, the files are loaded one by one while it
	 *         is written
	 */
	@Transactional
	public byte[] pdf(long carId, String username, boolean history, Optional<String> language)
	{
		return CarPapersPdfWriter.write(papers(carId, username, history, language));
	}

	private List<CarPapers.File> files(List<AttachmentEntity> attachments)
	{
		return attachments.stream()
				.map(attachment -> new CarPapers.File(attachment, () -> attachmentService.content(attachment)))
				.toList();
	}
}
