package de.codeflowwizardry.carledger.backup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.attachment.AttachmentService;
import de.codeflowwizardry.carledger.data.*;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.AttachmentRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillPaymentRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillRepository;
import de.codeflowwizardry.carledger.data.repository.ReminderRepository;
import de.codeflowwizardry.carledger.exception.AlreadyPresentException;
import de.codeflowwizardry.carledger.rest.records.CarInput;
import de.codeflowwizardry.carledger.rest.records.input.SettingsInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * The backup of the own data as ZIP: backup.json with the settings, cars,
 * bills, recurring costs and reminders, and the files (receipts, pictures).
 * A restore fills an account without cars, all at once or nothing.
 */
@ApplicationScoped
public class BackupService
{
	/**
	 * Guards against ZIP bombs, far above any real backup: what may be unpacked in
	 * total (not at once, the files are unpacked one by one) and per backup.json
	 */
	static final long MAX_UNPACKED = 1024L * 1024 * 1024;
	static final int MAX_ENTRIES = 100_000;
	static final long MAX_JSON = 16L * 1024 * 1024;

	private final AccountRepository accountRepository;
	private final CarRepository carRepository;
	private final BillRepository billRepository;
	private final AttachmentRepository attachmentRepository;
	private final RecurringBillRepository recurringBillRepository;
	private final RecurringBillPaymentRepository paymentRepository;
	private final ReminderRepository reminderRepository;
	private final AttachmentService attachmentService;
	private final ObjectMapper mapper;
	private final EntityManager em;

	@Inject
	public BackupService(AccountRepository accountRepository, CarRepository carRepository,
			BillRepository billRepository, AttachmentRepository attachmentRepository,
			RecurringBillRepository recurringBillRepository, RecurringBillPaymentRepository paymentRepository,
			ReminderRepository reminderRepository, AttachmentService attachmentService, ObjectMapper mapper,
			EntityManager em)
	{
		this.accountRepository = accountRepository;
		this.carRepository = carRepository;
		this.billRepository = billRepository;
		this.attachmentRepository = attachmentRepository;
		this.recurringBillRepository = recurringBillRepository;
		this.paymentRepository = paymentRepository;
		this.reminderRepository = reminderRepository;
		this.attachmentService = attachmentService;
		this.mapper = mapper;
		this.em = em;
	}

	/**
	 * @return the ZIP with all data of the user
	 */
	@Transactional
	public byte[] create(String user)
	{
		AccountEntity account = accountRepository.findByIdentifier(user);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(out))
		{
			Files files = new Files(zip);
			List<BackupFile.Car> cars = new ArrayList<>();
			for (CarEntity car : carRepository.listOwned(user))
			{
				cars.add(car(car, user, files));
			}
			BackupFile backup = new BackupFile(BackupFile.FORMAT, BackupFile.VERSION, OffsetDateTime.now(),
					new BackupFile.Settings(account.getVatCountry(), account.getCurrency(), account.getLanguage(),
							account.getUnitSystem()),
					cars);
			zip.putNextEntry(new ZipEntry(BackupFile.ENTRY));
			zip.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(backup));
			zip.closeEntry();
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return out.toByteArray();
	}

	/**
	 * Writes the content of the attachments as entries of the ZIP
	 */
	private final class Files
	{
		private final ZipOutputStream zip;
		private int next = 1;

		Files(ZipOutputStream zip)
		{
			this.zip = zip;
		}

		BackupFile.File add(AttachmentEntity attachment) throws IOException
		{
			String path = "files/" + next++;
			zip.putNextEntry(new ZipEntry(path));
			zip.write(attachmentService.content(attachment));
			zip.closeEntry();
			return new BackupFile.File(attachment.getFileName(), attachment.getContentType(), path);
		}
	}

	private BackupFile.Car car(CarEntity car, String user, Files files) throws IOException
	{
		Map<Long, List<AttachmentEntity>> attachments = attachmentRepository.list("car = ?1 order by id", car)
				.stream()
				.filter(attachment -> attachment.getBill() != null)
				.collect(Collectors.groupingBy(attachment -> attachment.getBill().getId()));

		List<BackupFile.Bill> bills = new ArrayList<>();
		List<BillEntity> entities = new ArrayList<>(
				billRepository.getBills(car.getId(), user, Optional.empty(), Optional.empty()));
		entities.sort(Comparator.comparing(BillEntity::getDate).thenComparing(BillEntity::getId));
		for (BillEntity bill : entities)
		{
			List<BackupFile.File> billFiles = new ArrayList<>();
			for (AttachmentEntity attachment : attachments.getOrDefault(bill.getId(), List.of()))
			{
				billFiles.add(files.add(attachment));
			}
			bills.add(bill(bill, billFiles));
		}

		List<BackupFile.RecurringBill> recurringBills = new ArrayList<>();
		for (RecurringBillEntity recurring : recurringBillRepository.list("car = ?1 order by id", car))
		{
			List<BackupFile.Payment> payments = paymentRepository.list("bill = ?1 order by dueDate, id", recurring)
					.stream()
					.map(payment -> new BackupFile.Payment(payment.getDueDate(), payment.getPaidDate(),
							payment.getAmount(), payment.isAutoGenerated()))
					.toList();
			recurringBills.add(new BackupFile.RecurringBill(recurring.getName(), recurring.getDescription(),
					recurring.getAmount(), recurring.getCategory(), recurring.getInterval(), recurring.getStartDate(),
					recurring.getNextDueDate(), recurring.getEndDate(), recurring.getTotal(), payments));
		}

		List<BackupFile.Reminder> reminders = reminderRepository.list(car.getId(), user).stream()
				.map(reminder -> new BackupFile.Reminder(reminder.getTitle(), reminder.getNote(),
						reminder.getDueDate(), reminder.getDueOdometer(), reminder.getIntervalMonths(),
						reminder.getIntervalKm()))
				.toList();

		BackupFile.File picture = null;
		if (car.getPictureId() != null)
		{
			Optional<AttachmentEntity> attachment = attachmentRepository.findByIdOptional(car.getPictureId());
			if (attachment.isPresent())
			{
				picture = files.add(attachment.get());
			}
		}

		return new BackupFile.Car(car.getName(), car.getManufactureYear(), car.getOdometer(), car.getOdometerDate(),
				car.getLicensePlate(), car.getVin(), car.getPurchaseDate(), car.getPurchasePrice(),
				car.getEnergyType(), car.getTankSize(), car.getBatteryCapacity(), picture, bills, recurringBills,
				reminders);
	}

	private static BackupFile.Bill bill(BillEntity bill, List<BackupFile.File> files)
	{
		FuelBillEntity fuel = bill.getFuelBill();
		ChargingBillEntity charging = bill.getChargingBill();
		MaintenanceBillEntity maintenance = bill.getMaintenanceBill();
		MiscellaneousBillEntity miscellaneous = bill.getMiscellaneousBill();
		return new BackupFile.Bill(bill.getType(), bill.getDate(), bill.getTotal(), bill.getVatRate(),
				bill.getNetAmount(), bill.getUstAmount(),
				fuel == null ? null
						: new BackupFile.Fuel(fuel.getUnit(), fuel.getPricePerUnit(), fuel.getDistance(),
								fuel.getEstimate(), fuel.getAvgConsumption(), fuel.getCostPerKm()),
				charging == null ? null
						: new BackupFile.Charging(charging.getUnit(), charging.getPricePerUnit(),
								charging.getDistance(), charging.getAvgConsumption(), charging.getCostPerKm(),
								charging.getLocation(), charging.getStateOfCharge()),
				maintenance == null ? null
						: new BackupFile.Maintenance(maintenance.getOdometer(), maintenance.getDescription(),
								maintenance.getWorkshop(), maintenance.getLaborCost(), maintenance.getPartsCost()),
				miscellaneous == null ? null : new BackupFile.Miscellaneous(miscellaneous.getDescription()),
				files);
	}

	/**
	 * Restores a backup into the account of the user, which must not have cars
	 *
	 * @throws jakarta.ws.rs.BadRequestException
	 *             if the file is no backup, of an unknown version, invalid or has
	 *             more cars than the account may have
	 * @throws AlreadyPresentException
	 *             if the account has cars already
	 */
	@Transactional
	public BackupSummary restore(String user, InputStream zip)
	{
		try (BackupArchive entries = new BackupArchive(zip, MAX_ENTRIES, MAX_UNPACKED))
		{
			return restore(user, entries);
		}
	}

	private BackupSummary restore(String user, BackupArchive entries)
	{
		BackupFile backup = parse(entries.read(BackupFile.ENTRY, MAX_JSON));

		AccountEntity account = accountRepository.findByIdentifier(user);
		if (carRepository.countOwned(user) > 0)
		{
			throw new AlreadyPresentException("A backup can only be restored into an account without cars!");
		}
		List<BackupFile.Car> cars = backup.cars() == null ? List.of() : backup.cars();
		if (cars.size() > account.getMaxCars())
		{
			Utils.requireValid(List.of("The backup has " + cars.size() + " cars, your account allows "
					+ account.getMaxCars() + ". Request more cars in the garage first."));
		}
		restoreSettings(account, backup.settings());

		Counter counter = new Counter();
		for (BackupFile.Car car : cars)
		{
			restoreCar(account, car, entries, counter);
		}
		return new BackupSummary(cars.size(), counter.bills, counter.recurringBills, counter.reminders,
				counter.files);
	}

	private static final class Counter
	{
		int bills;
		int recurringBills;
		int reminders;
		int files;
	}

	private BackupFile parse(byte[] json)
	{
		if (json == null)
		{
			Utils.requireValid(List.of("The file is no backup of CarLedger!"));
		}
		BackupFile backup;
		try
		{
			backup = mapper.readValue(json, BackupFile.class);
		}
		catch (IOException e)
		{
			Utils.requireValid(List.of("The backup cannot be read!"));
			throw new IllegalStateException(e);
		}
		if (!BackupFile.FORMAT.equals(backup.format()))
		{
			Utils.requireValid(List.of("The file is no backup of CarLedger!"));
		}
		if (backup.version() != BackupFile.VERSION)
		{
			Utils.requireValid(List.of("Backups of version " + backup.version() + " cannot be restored, only version "
					+ BackupFile.VERSION + "!"));
		}
		return backup;
	}

	private static void restoreSettings(AccountEntity account, BackupFile.Settings settings)
	{
		if (settings == null)
		{
			return;
		}
		SettingsInput input = new SettingsInput(settings.vatCountry(), settings.currency(), settings.language(),
				settings.unitSystem());
		Utils.requireValid(input.validate());
		account.setVatCountry(input.vatCountry());
		account.setCurrency(input.currency());
		account.setLanguage(input.language());
		if (input.unitSystem() != null)
		{
			account.setUnitSystem(input.unitSystem());
		}
	}

	private void restoreCar(AccountEntity account, BackupFile.Car backup, BackupArchive entries,
			Counter counter)
	{
		CarInput input = new CarInput(backup.name(), backup.year(), backup.odometer(), backup.licensePlate(),
				backup.vin(), backup.purchaseDate(), backup.purchasePrice(), backup.energyType(), backup.tankSize(),
				backup.batteryCapacity());
		List<String> errors = new ArrayList<>(input.validateDetails());
		if (StringUtils.isBlank(backup.name()) || backup.odometer() < 0)
		{
			errors.add("A car of the backup has no name or a negative odometer!");
		}
		Utils.requireValid(errors);

		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName(backup.name());
		car.setManufactureYear(backup.year());
		car.setOdometer(backup.odometer());
		if (backup.odometerDate() != null)
		{
			car.setOdometerDate(backup.odometerDate());
		}
		car.setLicensePlate(input.normalizedLicensePlate());
		car.setVin(input.normalizedVin());
		car.setPurchaseDate(backup.purchaseDate());
		car.setPurchasePrice(backup.purchasePrice());
		car.setEnergyType(input.energyTypeOrDefault());
		car.setTankSize(input.normalizedTankSize());
		car.setBatteryCapacity(input.normalizedBatteryCapacity());
		carRepository.persist(car);

		for (BackupFile.Bill bill : list(backup.bills()))
		{
			restoreBill(car, bill, entries, counter);
		}
		for (BackupFile.RecurringBill recurring : list(backup.recurringBills()))
		{
			restoreRecurringBill(car, recurring);
			counter.recurringBills++;
		}
		for (BackupFile.Reminder reminder : list(backup.reminders()))
		{
			ReminderEntity entity = new ReminderEntity(car);
			entity.setTitle(reminder.title());
			entity.setNote(reminder.note());
			entity.setDueDate(reminder.dueDate());
			entity.setDueOdometer(reminder.dueOdometer());
			entity.setIntervalMonths(reminder.intervalMonths());
			entity.setIntervalKm(reminder.intervalKm());
			reminderRepository.persist(entity);
			counter.reminders++;
		}
		if (backup.picture() != null)
		{
			car.setPictureId(attachmentService
					.restore(car, null, backup.picture().fileName(), content(entries, backup.picture())).getId());
			counter.files++;
		}
	}

	private void restoreBill(CarEntity car, BackupFile.Bill backup, BackupArchive entries, Counter counter)
	{
		if (backup.type() == null || backup.date() == null || backup.total() == null)
		{
			Utils.requireValid(List.of("An entry of the backup has no type, date or total!"));
		}
		BillEntity bill = new BillEntity();
		bill.setCar(car);
		bill.setType(backup.type());
		bill.setDate(backup.date());
		bill.setTotal(backup.total());
		bill.setVatRate(backup.vatRate());
		bill.setNetAmount(backup.netAmount() == null ? backup.total() : backup.netAmount());
		bill.setUstAmount(backup.vatAmount() == null ? BigDecimal.ZERO : backup.vatAmount());
		em.persist(bill);

		switch (backup.type())
		{
			case FUEL -> {
				BackupFile.Fuel fuel = require(backup.fuel());
				FuelBillEntity entity = new FuelBillEntity(bill);
				entity.setUnit(fuel.unit());
				entity.setPricePerUnit(fuel.pricePerUnit());
				entity.setDistance(fuel.distance());
				entity.setEstimate(fuel.estimate());
				entity.setAvgConsumption(fuel.avgConsumption());
				entity.setCostPerKm(fuel.costPerKm());
				em.persist(entity);
			}
			case CHARGING -> {
				BackupFile.Charging charging = require(backup.charging());
				ChargingBillEntity entity = new ChargingBillEntity(bill);
				entity.setUnit(charging.unit());
				entity.setPricePerUnit(charging.pricePerUnit());
				entity.setDistance(charging.distance());
				entity.setAvgConsumption(charging.avgConsumption());
				entity.setCostPerKm(charging.costPerKm());
				entity.setLocation(charging.location());
				entity.setStateOfCharge(charging.stateOfCharge());
				em.persist(entity);
			}
			case MAINTENANCE -> {
				BackupFile.Maintenance maintenance = require(backup.maintenance());
				MaintenanceBillEntity entity = new MaintenanceBillEntity(bill);
				entity.setOdometer(maintenance.odometer());
				entity.setDescription(maintenance.description());
				entity.setWorkshop(maintenance.workshop());
				entity.setLaborCost(maintenance.laborCost());
				entity.setPartsCost(maintenance.partsCost());
				em.persist(entity);
			}
			case MISCELLANEOUS -> {
				BackupFile.Miscellaneous miscellaneous = require(backup.miscellaneous());
				MiscellaneousBillEntity entity = new MiscellaneousBillEntity(bill);
				entity.setDescription(miscellaneous.description());
				em.persist(entity);
			}
			case RECURRING -> Utils.requireValid(List.of("Recurring costs are no entries of a backup!"));
		}
		counter.bills++;

		List<BackupFile.File> files = list(backup.attachments());
		if (files.size() > attachmentService.maxPerBill())
		{
			Utils.requireValid(List.of("An entry can have at most " + attachmentService.maxPerBill()
					+ " attachments, one of the backup has " + files.size() + "!"));
		}
		for (BackupFile.File file : files)
		{
			attachmentService.restore(car, bill, file.fileName(), content(entries, file));
			counter.files++;
		}
	}

	private void restoreRecurringBill(CarEntity car, BackupFile.RecurringBill backup)
	{
		if (StringUtils.isBlank(backup.name()) || backup.amount() == null || backup.category() == null
				|| backup.interval() == null || backup.startDate() == null)
		{
			Utils.requireValid(List.of("A recurring cost of the backup is incomplete!"));
		}
		RecurringBillEntity recurring = new RecurringBillEntity(car);
		recurring.setName(backup.name());
		recurring.setDescription(backup.description());
		recurring.setAmount(backup.amount());
		recurring.setCategory(backup.category());
		recurring.setInterval(backup.interval());
		recurring.setStartDate(backup.startDate());
		recurring.setNextDueDate(backup.nextDueDate() == null ? backup.startDate() : backup.nextDueDate());
		recurring.setEndDate(backup.endDate());
		recurring.setTotal(backup.total() == null ? BigDecimal.ZERO : backup.total());
		recurringBillRepository.persist(recurring);
		for (BackupFile.Payment payment : list(backup.payments()))
		{
			RecurringBillPaymentEntity entity = new RecurringBillPaymentEntity(recurring);
			entity.setDueDate(payment.dueDate());
			entity.setPaidDate(payment.paidDate() == null ? payment.dueDate() : payment.paidDate());
			entity.setAmount(payment.amount());
			entity.setAutoGenerated(payment.autoGenerated());
			paymentRepository.persist(entity);
		}
	}

	private byte[] content(BackupArchive entries, BackupFile.File file)
	{
		byte[] content = entries.read(file.path(), attachmentService.maxSize());
		if (content == null)
		{
			Utils.requireValid(List.of("The file " + file.fileName() + " is missing in the backup!"));
		}
		return content;
	}

	private static <T> T require(T part)
	{
		if (part == null)
		{
			Utils.requireValid(List.of("An entry of the backup has no details of its type!"));
		}
		return part;
	}

	private static <T> List<T> list(List<T> list)
	{
		return list == null ? List.of() : list;
	}
}
