package de.codeflowwizardry.carledger.backup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import de.codeflowwizardry.carledger.data.BillCategory;
import de.codeflowwizardry.carledger.data.BillInterval;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.ChargingLocation;
import de.codeflowwizardry.carledger.data.DocumentKind;
import de.codeflowwizardry.carledger.data.EnergyType;
import de.codeflowwizardry.carledger.data.InspectionResult;
import de.codeflowwizardry.carledger.data.ReminderKind;
import de.codeflowwizardry.carledger.data.TripReview;
import de.codeflowwizardry.carledger.data.TripType;
import de.codeflowwizardry.carledger.data.Accent;
import de.codeflowwizardry.carledger.data.ApprovalType;
import de.codeflowwizardry.carledger.data.UnitSystem;

/**
 * Content of backup.json in the ZIP of a backup. The files (receipts, pictures)
 * are entries of the ZIP, named by {@link File#path()}. The stored values are
 * kept as they are: km, liters, cent per liter and the calculated consumption.
 * New fields are optional, so older backups can still be restored (e.g. the
 * trips).
 *
 * @param format
 *            always {@value #FORMAT}
 * @param version
 *            raised with incompatible changes, a restore accepts only the
 *            versions it knows
 */
public record BackupFile(String format, int version, OffsetDateTime createdAt, Settings settings, List<Car> cars)
{
	public static final String FORMAT = "car-ledger-backup";
	public static final int VERSION = 1;
	/** name of this content in the ZIP */
	public static final String ENTRY = "backup.json";
	/**
	 * first line of the CSV of the track points, tracker is the index in
	 * {@link Car#trackers()}, times in ISO-8601, km/h and meters
	 */
	public static final String TRACK_HEADER = "tracker,ts,lat,lon,speed,alt,hdop";

	/**
	 * @param accent
	 *            missing in backups before the presets
	 */
	public record Settings(String vatCountry, String currency, String language, UnitSystem unitSystem,
			Accent accent)
	{
	}

	/**
	 * @param picture
	 *            the own picture, <code>null</code> for the generated avatar
	 * @param trips
	 *            missing in backups before the trip log
	 * @param tripLog
	 *            missing in backups before it could be turned off: on with
	 *            trips
	 * @param trackRetentionDays
	 *            days the track points are kept, forever without
	 * @param trackers
	 *            the GPS trackers, missing in backups before them
	 * @param track
	 *            CSV with the track points ({@link #TRACK_HEADER}), only for a
	 *            car that keeps them forever and has some
	 * @param modifications
	 *            missing in backups before them
	 * @param technicalInspections
	 *            the HUs, missing in backups before them
	 * @param places
	 *            the named places, missing in backups before them
	 */
	public record Car(String name, int year, int odometer, LocalDate odometerDate, String licensePlate, String vin,
			LocalDate purchaseDate, BigDecimal purchasePrice, EnergyType energyType, BigDecimal tankSize,
			BigDecimal batteryCapacity, File picture, List<Bill> bills, List<RecurringBill> recurringBills,
			List<Reminder> reminders, List<Trip> trips, Boolean tripLog, Integer trackRetentionDays,
			List<Tracker> trackers, File track, List<Modification> modifications,
			List<TechnicalInspection> technicalInspections, List<Place> places)
	{
	}

	/**
	 * A place the owner named, e.g. a fuel station
	 */
	public record Place(String name, double lat, double lon)
	{
	}

	/**
	 * A GPS tracker. Its token isn't in the backup, only the SHA-256 the server
	 * stores: after a restore on another server it keeps sending.
	 */
	public record Tracker(String name, String tokenHash, OffsetDateTime createdAt, OffsetDateTime lastSeenAt,
			OffsetDateTime revokedAt)
	{
	}

	/**
	 * @param path
	 *            entry of the ZIP with the content
	 * @param kind
	 *            of a document of a modification, missing for other files
	 */
	public record File(String fileName, String contentType, String path, DocumentKind kind)
	{
	}

	/**
	 * One of fuel, charging, maintenance and miscellaneous is set, by the type
	 */
	public record Bill(BillType type, LocalDate date, BigDecimal total, int vatRate, BigDecimal netAmount,
			BigDecimal vatAmount, Fuel fuel, Charging charging, Maintenance maintenance, Miscellaneous miscellaneous,
			List<File> attachments)
	{
	}

	public record Fuel(BigDecimal unit, BigDecimal pricePerUnit, BigDecimal distance, BigDecimal estimate,
			BigDecimal avgConsumption, BigDecimal costPerKm)
	{
	}

	public record Charging(BigDecimal unit, BigDecimal pricePerUnit, BigDecimal distance, BigDecimal avgConsumption,
			BigDecimal costPerKm, ChargingLocation location, Integer stateOfCharge)
	{
	}

	public record Maintenance(Integer odometer, String description, String workshop, BigDecimal laborCost,
			BigDecimal partsCost)
	{
	}

	public record Miscellaneous(String description)
	{
	}

	/**
	 * @param files
	 *            e.g. the contract, missing in backups before them
	 */
	public record RecurringBill(String name, String description, BigDecimal amount, BillCategory category,
			BillInterval interval, LocalDate startDate, LocalDate nextDueDate, LocalDate endDate, BigDecimal total,
			List<Payment> payments, List<File> files)
	{
	}

	public record Payment(LocalDate dueDate, LocalDate paidDate, BigDecimal amount, boolean autoGenerated)
	{
	}

	/**
	 * @param kind
	 *            of a reminder the app keeps, e.g. of the HU
	 */
	public record Reminder(String title, String note, LocalDate dueDate, Integer dueOdometer, Integer intervalMonths,
			Integer intervalKm, ReminderKind kind)
	{
	}

	/**
	 * An HU
	 *
	 * @param files
	 *            e.g. the report
	 */
	public record TechnicalInspection(LocalDate date, Integer odometer, String testingBody,
			InspectionResult result, String defects, LocalDate nextDue, List<File> files)
	{
	}

	/**
	 * A trip without its id, the restore creates new ones. The end is missing
	 * while the trip is open, the deletion of one that wasn't deleted (and in
	 * backups before trips could be deleted).
	 */
	public record Trip(TripType type, int startOdometer, Integer endOdometer, OffsetDateTime startedAt,
			OffsetDateTime endedAt, String destination, String purpose, String businessPartner,
			TripReview reviewReason, OffsetDateTime createdAt, OffsetDateTime finishedAt, OffsetDateTime deletedAt,
			String deletedBy, String deleteReason)
	{
	}

	/**
	 * @param bills
	 *            indexes in {@link Car#bills()}, e.g. of the part, the workshop and
	 *            the Abnahme
	 * @param bill
	 *            the one index of the prereleases of 0.8.0, before several bills
	 * @param documents
	 *            missing in backups before them
	 */
	public record Modification(String part, String manufacturer, ApprovalType approvalType, String approvalNumber,
			String conditions, LocalDate inspectionDate, String testingBody, boolean registered,
			LocalDate installedDate, Integer installedOdometer, LocalDate removedDate, Integer removedOdometer,
			String notes, List<Integer> bills, Integer bill, List<File> documents)
	{
	}
}
