package de.codeflowwizardry.carledger.backup;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
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
import de.codeflowwizardry.carledger.data.repository.DeviceRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillPaymentRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillRepository;
import de.codeflowwizardry.carledger.data.repository.ReminderRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
import de.codeflowwizardry.carledger.data.repository.TripRepository;
import de.codeflowwizardry.carledger.exception.AlreadyPresentException;
import de.codeflowwizardry.carledger.rest.car.track.TrackRetentionInput;
import de.codeflowwizardry.carledger.rest.car.tracker.DeviceInput;
import de.codeflowwizardry.carledger.rest.car.trip.TripDeleteInput;
import de.codeflowwizardry.carledger.rest.car.trip.TripFinishInput;
import de.codeflowwizardry.carledger.rest.car.trip.TripStartInput;
import de.codeflowwizardry.carledger.rest.records.CarInput;
import de.codeflowwizardry.carledger.rest.device.TrackPointInput;
import de.codeflowwizardry.carledger.rest.records.input.SettingsInput;
import de.codeflowwizardry.carledger.tracker.DeviceTokens;
import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * The backup of the own data as ZIP: backup.json with the settings, cars,
 * bills, recurring costs, reminders, trips and GPS trackers, and the files
 * (receipts, pictures, the track points of the cars that keep them forever). A
 * restore fills an account without cars, all at once or nothing.
 * Restored trips are marked, as a backup is a file anyone can edit.
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
	/**
	 * seconds of the transaction of a backup or a restore, a year of track points
	 * takes a while
	 */
	static final String TIMEOUT = "carledger.backup.transaction-timeout";
	private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

	private final AccountRepository accountRepository;
	private final CarRepository carRepository;
	private final BillRepository billRepository;
	private final AttachmentRepository attachmentRepository;
	private final RecurringBillRepository recurringBillRepository;
	private final RecurringBillPaymentRepository paymentRepository;
	private final ReminderRepository reminderRepository;
	private final TripRepository tripRepository;
	private final DeviceRepository deviceRepository;
	private final TrackPointRepository trackPointRepository;
	private final AttachmentService attachmentService;
	private final ObjectMapper mapper;
	private final EntityManager em;

	@Inject
	public BackupService(AccountRepository accountRepository, CarRepository carRepository,
			BillRepository billRepository, AttachmentRepository attachmentRepository,
			RecurringBillRepository recurringBillRepository, RecurringBillPaymentRepository paymentRepository,
			ReminderRepository reminderRepository, TripRepository tripRepository, DeviceRepository deviceRepository,
			TrackPointRepository trackPointRepository, AttachmentService attachmentService, ObjectMapper mapper,
			EntityManager em)
	{
		this.accountRepository = accountRepository;
		this.carRepository = carRepository;
		this.billRepository = billRepository;
		this.attachmentRepository = attachmentRepository;
		this.recurringBillRepository = recurringBillRepository;
		this.paymentRepository = paymentRepository;
		this.reminderRepository = reminderRepository;
		this.tripRepository = tripRepository;
		this.deviceRepository = deviceRepository;
		this.trackPointRepository = trackPointRepository;
		this.attachmentService = attachmentService;
		this.mapper = mapper;
		this.em = em;
	}

	/**
	 * Writes the ZIP with all data of the user, as it goes: a year of track points
	 * is never in memory at once
	 */
	@Transactional
	@TransactionConfiguration(timeoutFromConfigProperty = TIMEOUT)
	public void create(String user, OutputStream out)
	{
		AccountEntity account = accountRepository.findByIdentifier(user);
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
							account.getUnitSystem(), account.getAccent()),
					cars);
			zip.putNextEntry(new ZipEntry(BackupFile.ENTRY));
			zip.write(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(backup));
			zip.closeEntry();
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
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

		/**
		 * @param trackers
		 *            index of each tracker in the backup, by its id
		 * @return the CSV with the points, <code>null</code> without any
		 */
		BackupFile.File track(CarEntity car, Map<Long, Integer> trackers) throws IOException
		{
			if (!trackPointRepository.exists(car.getId()))
			{
				return null;
			}
			String path = "tracks/" + next++;
			zip.putNextEntry(new ZipEntry(path));
			zip.write((BackupFile.TRACK_HEADER + "\n").getBytes(StandardCharsets.UTF_8));
			// straight from the database, the ZIP stays open for the next entries
			trackPointRepository.copyOut(car.getId(), trackers, new FilterOutputStream(zip) {
				@Override
				public void write(byte[] bytes, int offset, int length) throws IOException
				{
					out.write(bytes, offset, length);
				}

				@Override
				public void close()
				{
				}
			});
			zip.closeEntry();
			return new BackupFile.File(null, "text/csv", path);
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

		List<BackupFile.Trip> trips = tripRepository.list("car = ?1 order by startedAt, id", car).stream()
				.map(trip -> new BackupFile.Trip(trip.getType(), trip.getStartOdometer(), trip.getEndOdometer(),
						trip.getStartedAt(), trip.getEndedAt(), trip.getDestination(), trip.getPurpose(),
						trip.getBusinessPartner(), trip.getReviewReason(), trip.getCreatedAt(),
						trip.getFinishedAt(), trip.getDeletedAt(), trip.getDeletedBy(), trip.getDeleteReason()))
				.toList();

		List<DeviceEntity> devices = deviceRepository.list("car = ?1 order by id", car);
		Map<Long, Integer> indexes = new HashMap<>();
		List<BackupFile.Tracker> trackers = new ArrayList<>();
		for (DeviceEntity device : devices)
		{
			indexes.put(device.getId(), trackers.size());
			trackers.add(new BackupFile.Tracker(device.getName(), device.getTokenHash(), device.getCreatedAt(),
					device.getLastSeenAt(), device.getRevokedAt()));
		}
		// points kept for a while are left out, a backup kept for longer would keep
		// them
		BackupFile.File track = car.getTrackRetentionDays() == null && !devices.isEmpty()
				? files.track(car, indexes)
				: null;

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
				reminders, trips, car.isTripLog(), car.getTrackRetentionDays(), trackers, track);
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
	@TransactionConfiguration(timeoutFromConfigProperty = TIMEOUT)
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
				counter.trips, counter.files, counter.trackers, counter.trackPoints);
	}

	private static final class Counter
	{
		int bills;
		int recurringBills;
		int reminders;
		int trips;
		int files;
		int trackers;
		int trackPoints;
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
				settings.unitSystem(), settings.accent());
		Utils.requireValid(input.validate());
		account.setVatCountry(input.vatCountry());
		account.setCurrency(input.currency());
		account.setLanguage(input.language());
		if (input.unitSystem() != null)
		{
			account.setUnitSystem(input.unitSystem());
		}
		if (input.accent() != null)
		{
			account.setAccent(input.accent());
		}
	}

	private void restoreCar(AccountEntity account, BackupFile.Car backup, BackupArchive entries,
			Counter counter)
	{
		CarInput input = new CarInput(backup.name(), backup.year(), backup.odometer(), backup.licensePlate(),
				backup.vin(), backup.purchaseDate(), backup.purchasePrice(), backup.energyType(), backup.tankSize(),
				backup.batteryCapacity(), backup.tripLog());
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
		car.setTripLog(backup.tripLog() != null ? backup.tripLog() : !list(backup.trips()).isEmpty());
		Utils.requireValid(new TrackRetentionInput(backup.trackRetentionDays()).validate());
		car.setTrackRetentionDays(backup.trackRetentionDays());
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
		boolean open = false;
		for (BackupFile.Trip trip : list(backup.trips()))
		{
			// a deleted open trip doesn't block the next one
			if (trip.endOdometer() == null && trip.endedAt() == null && trip.deletedAt() == null)
			{
				if (open)
				{
					Utils.requireValid(List.of("A car of the backup has more than one open trip!"));
				}
				open = true;
			}
			restoreTrip(account, car, trip);
			counter.trips++;
		}
		List<Long> devices = new ArrayList<>();
		for (BackupFile.Tracker tracker : list(backup.trackers()))
		{
			devices.add(restoreTracker(car, tracker));
			counter.trackers++;
		}
		if (backup.track() != null)
		{
			counter.trackPoints += restoreTrack(car, devices, backup.track(), entries);
		}
		if (backup.picture() != null)
		{
			car.setPictureId(attachmentService
					.restore(car, null, backup.picture().fileName(), content(entries, backup.picture())).getId());
			counter.files++;
		}
	}

	/**
	 * With the hash of its token, so it keeps sending. A hash that another tracker
	 * has already (e.g. the account of the backup still exists) or that is no
	 * SHA-256 gets a random one: the owner issues a new token.
	 *
	 * @return the id of the tracker
	 */
	private long restoreTracker(CarEntity car, BackupFile.Tracker backup)
	{
		List<String> errors = new ArrayList<>(new DeviceInput(backup.name()).validate());
		if (backup.createdAt() == null)
		{
			errors.add("A tracker of the backup has no creation time!");
		}
		Utils.requireValid(errors);

		String hash = backup.tokenHash();
		if (hash == null || !SHA_256.matcher(hash).matches() || deviceRepository.existsByTokenHash(hash))
		{
			hash = DeviceTokens.hash(DeviceTokens.generate());
		}
		DeviceEntity device = new DeviceEntity(car, backup.name().strip(), hash, backup.createdAt(),
				backup.lastSeenAt(), backup.revokedAt());
		deviceRepository.persist(device);
		return device.getId();
	}

	/**
	 * The points of the CSV, invalid ones are skipped like the ones a tracker
	 * sends
	 *
	 * @param devices
	 *            ids of the restored trackers, in the order of the backup
	 * @return the amount of points restored
	 */
	private int restoreTrack(CarEntity car, List<Long> devices, BackupFile.File track, BackupArchive entries)
	{
		// the points refer to the car and the trackers by id, outside of Hibernate
		em.flush();
		OffsetDateTime now = OffsetDateTime.now();
		boolean[] header = {
				true
		};
		try (TrackPointRepository.Bulk bulk = trackPointRepository.bulk(car.getId()))
		{
			boolean found = entries.readLines(track.path(), line -> {
				if (header[0])
				{
					if (!BackupFile.TRACK_HEADER.equals(line.strip()))
					{
						Utils.requireValid(List.of("The track of a car of the backup cannot be read!"));
					}
					header[0] = false;
					return;
				}
				if (line.isBlank())
				{
					return;
				}
				String[] values = line.split(",", -1);
				long device;
				TrackPointInput point;
				try
				{
					if (values.length != 7)
					{
						throw new IllegalArgumentException("7 values expected");
					}
					device = devices.get(Integer.parseInt(values[0]));
					point = new TrackPointInput(time(values[1]), Double.valueOf(values[2]),
							Double.valueOf(values[3]), number(values[4]), number(values[5]), number(values[6]));
				}
				catch (RuntimeException e)
				{
					Utils.requireValid(List.of("The track of a car of the backup cannot be read!"));
					throw e;
				}
				if (point.isValid(now))
				{
					bulk.add(device, point);
				}
			});
			if (!found)
			{
				Utils.requireValid(List.of("The file " + track.path() + " is missing in the backup!"));
			}
			return bulk.finish();
		}
	}

	/**
	 * @return the time of epoch seconds, or ISO-8601 as in the backups of the dev
	 *         builds before 0.7.0
	 */
	private static OffsetDateTime time(String value)
	{
		if (value.indexOf('T') >= 0)
		{
			return OffsetDateTime.parse(value);
		}
		BigDecimal seconds = new BigDecimal(value);
		return Instant.ofEpochSecond(seconds.longValue(),
				seconds.remainder(BigDecimal.ONE).movePointRight(9).longValue()).atOffset(ZoneOffset.UTC);
	}

	private static Double number(String value)
	{
		return value.isEmpty() ? null : Double.valueOf(value);
	}

	/**
	 * With a new id and the times of the backup, marked as restored
	 */
	private void restoreTrip(AccountEntity account, CarEntity car, BackupFile.Trip backup)
	{
		List<String> errors = new ArrayList<>(new TripStartInput(backup.type(), backup.startOdometer(),
				backup.startedAt(), backup.destination(), backup.purpose(), backup.businessPartner(), false)
				.validate());
		boolean finished = backup.endOdometer() != null || backup.endedAt() != null;
		if (finished)
		{
			errors.addAll(new TripFinishInput(backup.endOdometer(), backup.endedAt(), null, null, null, false)
					.validate());
			if (errors.isEmpty() && (backup.endOdometer() < backup.startOdometer()
					|| backup.endedAt().isBefore(backup.startedAt())))
			{
				errors.add("A trip of the backup ends before its start!");
			}
		}
		boolean deleted = backup.deletedAt() != null;
		if (deleted != (backup.deletedBy() != null) || deleted != (backup.deleteReason() != null))
		{
			errors.add("A deleted trip of the backup needs the time, the account and the reason of the deletion!");
		} else if (deleted)
		{
			errors.addAll(new TripDeleteInput(backup.deleteReason()).validate());
		}
		Utils.requireValid(errors);

		OffsetDateTime createdAt = backup.createdAt() == null ? backup.startedAt() : backup.createdAt();
		TripEntity entity = new TripEntity(UUID.randomUUID(), car, account, backup.type(), backup.startOdometer(),
				backup.startedAt(), true, createdAt);
		entity.setDestination(StringUtils.trimToNull(backup.destination()));
		entity.setPurpose(StringUtils.trimToNull(backup.purpose()));
		entity.setBusinessPartner(StringUtils.trimToNull(backup.businessPartner()));
		entity.setReviewReason(backup.reviewReason());
		if (finished)
		{
			entity.finish(backup.endOdometer(), backup.endedAt(),
					backup.finishedAt() == null ? backup.endedAt() : backup.finishedAt());
		}
		if (deleted)
		{
			entity.delete(backup.deletedAt(), backup.deletedBy(), backup.deleteReason().strip());
		}
		tripRepository.persist(entity);
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
