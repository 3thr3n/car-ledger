package de.codeflowwizardry.carledger.backup;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.codeflowwizardry.carledger.ModificationService;
import de.codeflowwizardry.carledger.ReminderService;
import de.codeflowwizardry.carledger.TechnicalInspectionService;
import de.codeflowwizardry.carledger.TripService;
import de.codeflowwizardry.carledger.attachment.AttachmentService;
import de.codeflowwizardry.carledger.data.Accent;
import de.codeflowwizardry.carledger.data.ApprovalType;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.BillCategory;
import de.codeflowwizardry.carledger.data.BillInterval;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.ChargingLocation;
import de.codeflowwizardry.carledger.data.DocumentKind;
import de.codeflowwizardry.carledger.data.EnergyType;
import de.codeflowwizardry.carledger.data.InspectionResult;
import de.codeflowwizardry.carledger.data.TripType;
import de.codeflowwizardry.carledger.data.factory.ChargingBillFactory;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.factory.MaintenanceBillFactory;
import de.codeflowwizardry.carledger.data.factory.MiscellaneousBillFactory;
import de.codeflowwizardry.carledger.data.factory.RecurringBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.charging.ChargingBillInput;
import de.codeflowwizardry.carledger.rest.car.fuel.FuelBillInput;
import de.codeflowwizardry.carledger.rest.car.maintenance.MaintenanceBillInput;
import de.codeflowwizardry.carledger.rest.car.inspection.TechnicalInspectionInput;
import de.codeflowwizardry.carledger.rest.car.miscellaneous.MiscellaneousBillInput;
import de.codeflowwizardry.carledger.rest.car.modification.ModificationInput;
import de.codeflowwizardry.carledger.rest.car.recurring.RecurringBillInput;
import de.codeflowwizardry.carledger.rest.car.track.PlaceInput;
import de.codeflowwizardry.carledger.rest.car.reminder.ReminderInput;
import de.codeflowwizardry.carledger.rest.car.trip.TripFinishInput;
import de.codeflowwizardry.carledger.rest.car.trip.TripStartInput;
import de.codeflowwizardry.carledger.tracker.Places;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Peter backs up his plug-in hybrid with everything it can have and restores
 * it into his empty account
 */
@QuarkusTest
@TestSecurity(user = "peter", roles = "user")
class BackupTest
{
	/** the start of a PNG is enough for the type detection */
	private static final byte[] PNG = {
			(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3, 4
	};
	private static final byte[] PDF = "%PDF-1.4 receipt".getBytes(StandardCharsets.US_ASCII);

	@Inject
	AccountRepository accountRepository;
	@Inject
	CarRepository carRepository;
	@Inject
	FuelBillFactory fuelBillFactory;
	@Inject
	ChargingBillFactory chargingBillFactory;
	@Inject
	MaintenanceBillFactory maintenanceBillFactory;
	@Inject
	MiscellaneousBillFactory miscellaneousBillFactory;
	@Inject
	RecurringBillFactory recurringBillFactory;
	@Inject
	ReminderService reminderService;
	@Inject
	TripService tripService;
	@Inject
	ModificationService modificationService;
	@Inject
	TechnicalInspectionService technicalInspectionService;
	@Inject
	AttachmentService attachmentService;
	@Inject
	Places places;
	@Inject
	ObjectMapper mapper;

	long carId;

	@BeforeEach
	void setup()
	{
		carId = createAccountAndCar();
		long fuel = fuelBillFactory.create(new FuelBillInput(LocalDate.of(2025, 2, 10), null, 19,
				BigDecimal.valueOf(600), BigDecimal.valueOf(40), BigDecimal.valueOf(175), BigDecimal.valueOf(6.5)),
				carId, "peter").getBill().getId();
		attachmentService.addToBill(carId, fuel, "peter", "receipt.pdf", PDF);
		attachmentService.addToBill(carId, fuel, "peter", "pump.png", PNG);
		chargingBillFactory.create(new ChargingBillInput(LocalDate.of(2025, 2, 12), null, 19,
				BigDecimal.valueOf(50), BigDecimal.valueOf(9), BigDecimal.valueOf(30), ChargingLocation.HOME, 90),
				carId, "peter");
		long maintenance = maintenanceBillFactory.create(new MaintenanceBillInput(BigDecimal.valueOf(119), 19,
				LocalDate.of(2025, 1, 20), 41_000, BigDecimal.valueOf(80), BigDecimal.valueOf(39), "Oil change",
				"Garage"), carId, "peter").getBill().getId();
		miscellaneousBillFactory.create(
				new MiscellaneousBillInput(BigDecimal.TEN, 19, LocalDate.of(2025, 3, 1), "Car wash"), carId, "peter");
		long insurance = recurringBillFactory.create(carId, new RecurringBillInput("Insurance", "Full",
				BillInterval.MONTHLY, BillCategory.INSURANCE, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1),
				BigDecimal.valueOf(45)), "peter").getId();
		attachmentService.addToRecurringBill(carId, insurance, "peter", "policy.pdf", PDF);
		reminderService.create(carId, new ReminderInput("Service", "Oil", LocalDate.of(2026, 5, 1), 60_000, 12,
				15_000), "peter");
		attachmentService.setCarPicture(carId, "peter", "golf.png", PNG);
		long coilovers = modificationService.create(carId,
				new ModificationInput("Coilovers", "KW", ApprovalType.PARTS_REPORT,
						"TG 123", "Only with the rims of the Gutachten", LocalDate.of(2025, 1, 25), "TÜV Süd", true,
						LocalDate.of(2025, 1, 20), 41_000, null, null, null, List.of(maintenance)),
				"peter").id();
		attachmentService.addToModification(carId, coilovers, "peter", "gutachten.pdf", DocumentKind.APPROVAL, PDF);

		long hu = technicalInspectionService.create(carId, new TechnicalInspectionInput(LocalDate.of(2025, 3, 3),
				41_300, "DEKRA", InspectionResult.MINOR_DEFECTS, "Wiper blades", LocalDate.of(2027, 3, 1)), "peter")
				.id();
		attachmentService.addToTechnicalInspection(carId, hu, "peter", "hu.pdf", PDF);
		places.name(carId, new PlaceInput(48.1, 11.5, "Fuel station"), "peter");

		UUID business = UUID.randomUUID();
		tripService.start(carId, business, new TripStartInput(TripType.BUSINESS, 41_500,
				OffsetDateTime.parse("2025-03-02T08:00:00+01:00"), "Munich", null, null, false), "peter");
		tripService.finish(carId, business, new TripFinishInput(41_620,
				OffsetDateTime.parse("2025-03-02T10:30:00+01:00"), null, "Customer visit", "ACME", false), "peter");
		tripService.start(carId, UUID.randomUUID(), new TripStartInput(TripType.PRIVATE, 41_700,
				OffsetDateTime.parse("2025-03-05T17:00:00+01:00"), null, null, null, false), "peter");
	}

	@Transactional
	long createAccountAndCar()
	{
		accountRepository.deleteAll();
		AccountEntity account = new AccountEntity();
		account.setUserId("peter");
		account.setMaxCars(1);
		account.setCurrency("CHF");
		account.setAccent(Accent.VIOLET);
		accountRepository.persist(account);

		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName("Golf GTE");
		car.setManufactureYear(2020);
		car.setOdometer(40_000);
		car.setOdometerDate(LocalDate.of(2025, 1, 1));
		car.setVin("WVWZZZ1KZ6W000001");
		car.setEnergyType(EnergyType.PLUG_IN_HYBRID);
		car.setTankSize(BigDecimal.valueOf(40));
		car.setBatteryCapacity(BigDecimal.valueOf(13));
		car.setTripLog(true);
		carRepository.persist(car);
		return car.getId();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	private byte[] download()
	{
		return given()
				.when()
				.get("/api/user/me/backup")
				.then()
				.statusCode(200)
				.contentType("application/zip")
				.header("Content-Disposition", containsString("car-ledger-backup-peter-"))
				.extract()
				.asByteArray();
	}

	private static Map<String, byte[]> unzip(byte[] zip) throws IOException
	{
		Map<String, byte[]> entries = new HashMap<>();
		try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip)))
		{
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null)
			{
				entries.put(entry.getName(), in.readAllBytes());
			}
		}
		return entries;
	}

	private static byte[] zip(Map<String, byte[]> entries) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(out))
		{
			for (var entry : entries.entrySet())
			{
				zip.putNextEntry(new ZipEntry(entry.getKey()));
				zip.write(entry.getValue());
				zip.closeEntry();
			}
		}
		return out.toByteArray();
	}

	private io.restassured.response.ValidatableResponse restore(byte[] zip)
	{
		return given()
				.contentType(ContentType.MULTIPART)
				.multiPart("file", "backup.zip", zip, "application/zip")
				.when()
				.post("/api/user/me/backup")
				.then();
	}

	private void deleteCar()
	{
		given().when().delete("/api/car/my/{id}", carId).then().statusCode(204);
	}

	@Test
	void shouldWriteEverythingIntoTheZip() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());

		JsonNode backup = mapper.readTree(entries.get("backup.json"));
		assertEquals("car-ledger-backup", backup.get("format").asText());
		assertEquals(1, backup.get("version").asInt());
		assertEquals("CHF", backup.get("settings").get("currency").asText());
		assertEquals("VIOLET", backup.get("settings").get("accent").asText());
		JsonNode car = backup.get("cars").get(0);
		assertEquals("Golf GTE", car.get("name").asText());
		assertEquals("PLUG_IN_HYBRID", car.get("energyType").asText());
		// oldest first
		assertEquals("MAINTENANCE", car.get("bills").get(0).get("type").asText());
		assertEquals(4, car.get("bills").size());
		JsonNode fuel = car.get("bills").get(1);
		assertEquals(6.67, fuel.get("fuel").get("avgConsumption").asDouble());
		assertEquals(2, fuel.get("attachments").size());
		assertArrayEquals(PDF, entries.get(fuel.get("attachments").get(0).get("path").asText()));
		assertArrayEquals(PNG, entries.get(car.get("picture").get("path").asText()));
		assertEquals(2, car.get("recurringBills").get(0).get("payments").size());
		assertEquals("Fuel station", car.get("places").get(0).get("name").asText());
		assertEquals(48.1, car.get("places").get(0).get("lat").asDouble());
		JsonNode policy = car.get("recurringBills").get(0).get("files").get(0);
		assertEquals("policy.pdf", policy.get("fileName").asText());
		assertArrayEquals(PDF, entries.get(policy.get("path").asText()));
		assertEquals(15_000, car.get("reminders").get(0).get("intervalKm").asInt());
		JsonNode hu = car.get("technicalInspections").get(0);
		assertEquals("MINOR_DEFECTS", hu.get("result").asText());
		assertEquals("2027-03-01", hu.get("nextDue").asText());
		assertArrayEquals(PDF, entries.get(hu.get("files").get(0).get("path").asText()));
		// the reminder of the HU, kept by the app
		boolean kept = false;
		for (JsonNode reminder : car.get("reminders"))
		{
			kept |= "TECHNICAL_INSPECTION".equals(reminder.path("kind").asText());
		}
		assertTrue(kept);
		// the bill by its index, the maintenance is the oldest
		JsonNode modification = car.get("modifications").get(0);
		assertEquals("PARTS_REPORT", modification.get("approvalType").asText());
		assertEquals("TÜV Süd", modification.get("testingBody").asText());
		assertEquals(0, modification.get("bills").get(0).asInt());
		JsonNode document = modification.get("documents").get(0);
		assertEquals("APPROVAL", document.get("kind").asText());
		assertArrayEquals(PDF, entries.get(document.get("path").asText()));
		// oldest first, without the ids
		assertEquals(2, car.get("trips").size());
		JsonNode trip = car.get("trips").get(0);
		assertEquals("BUSINESS", trip.get("type").asText());
		assertEquals(41_620, trip.get("endOdometer").asInt());
		assertEquals("ACME", trip.get("businessPartner").asText());
		assertTrue(trip.get("id") == null);
		assertTrue(car.get("trips").get(1).get("endOdometer").isNull());
	}

	@Test
	void shouldRestoreTheBackupIntoAnEmptyAccount()
	{
		byte[] backup = download();
		deleteCar();

		restore(backup)
				.statusCode(200)
				.body("cars", is(1))
				.body("bills", is(4))
				.body("recurringBills", is(1))
				.body("trips", is(2))
				.body("modifications", is(1))
				.body("technicalInspections", is(1))
				.body("places", is(1))
				.body("reminders", is(2))
				.body("files", is(6));

		long restored = given().when().get("/api/car/my").then().statusCode(200)
				.body("name", contains("Golf GTE"))
				.body("[0].pictureId", notNullValue())
				.body("[0].vin", is("WVWZZZ1KZ6W000001"))
				.body("[0].batteryCapacity", is(13.0F))
				.body("[0].tripLog", is(true))
				.extract().jsonPath().getLong("[0].id");

		given().when().get("/api/bill/{carId}/fuel/all", restored).then()
				.body("data[0].avgConsumption", is(6.67F))
				.body("data[0].estimateConsumption", is(6.5F));
		given().when().get("/api/bill/{carId}/charging/all", restored).then()
				.body("data[0].location", is("HOME"))
				.body("data[0].stateOfCharge", is(90));
		given().when().get("/api/bill/{carId}/maintenance/all", restored).then()
				.body("data[0].odometer", is(41_000))
				.body("data[0].workshop", is("Garage"));
		given().when().get("/api/bill/{carId}/recurring", restored).then()
				.body("data[0].name", is("Insurance"))
				.body("data[0].total", is(90.0F))
				.body("data[0].attachments", is(1));
		given().when().get("/api/reminder/{carId}", restored).then()
				.body("title", containsInAnyOrder("Service", "HU"));
		given().when().get("/api/technical-inspection/{carId}", restored).then()
				.body("[0].defects", is("Wiper blades"))
				.body("[0].documents", is(1));
		long maintenance = given().when().get("/api/bill/{carId}/maintenance/all", restored).then()
				.extract().jsonPath().getLong("data[0].id");
		given().when().get("/api/modification/{carId}", restored).then()
				.body("[0].part", is("Coilovers"))
				.body("[0].registered", is(true))
				.body("[0].installedOdometer", is(41_000))
				.body("[0].bills.id", contains((int) maintenance))
				.body("[0].documents", is(1));
		long modification = given().when().get("/api/modification/{carId}", restored).then()
				.extract().jsonPath().getLong("[0].id");
		given().when().get("/api/attachment/{carId}/modification/{id}", restored, modification).then()
				.body("kind", contains("APPROVAL"))
				.body("fileName", contains("gutachten.pdf"));
		// the latest first, marked as restored, the times of the backup
		given().when().get("/api/trip/{carId}", restored).then()
				.body("total", is(2))
				.body("data.restored", contains(true, true))
				.body("data[0].endOdometer", is((Object) null))
				.body("data[1].distance", is(120))
				.body("data[1].purpose", is("Customer visit"))
				.body("data[1].endedAt", is("2025-03-02T09:30:00Z"));
		given().when().get("/api/trip/{carId}/current", restored).then()
				.body("open.startOdometer", is(41_700));
		byte[] picture = given().when().get("/api/car/my/{id}/picture", restored).then().statusCode(200)
				.extract().asByteArray();
		assertArrayEquals(PNG, picture);
	}

	@Test
	void shouldRestoreABackupWithoutTrips() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		ObjectNode json = (ObjectNode) mapper.readTree(entries.get("backup.json"));
		((ObjectNode) json.withArray("cars").get(0)).remove(List.of("trips", "tripLog"));
		entries.put("backup.json", mapper.writeValueAsBytes(json));

		restore(zip(entries)).statusCode(200).body("trips", is(0));
		given().when().get("/api/car/my").then().body("[0].tripLog", is(false));
	}

	@Test
	void shouldTurnTheTripLogOnForTripsOfABackupWithoutTheSetting() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		ObjectNode json = (ObjectNode) mapper.readTree(entries.get("backup.json"));
		((ObjectNode) json.withArray("cars").get(0)).remove("tripLog");
		entries.put("backup.json", mapper.writeValueAsBytes(json));

		restore(zip(entries)).statusCode(200).body("trips", is(2));
		given().when().get("/api/car/my").then().body("[0].tripLog", is(true));
	}

	@Test
	void shouldRestoreNothingWithAModificationOfAnUnknownBill() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		ObjectNode json = (ObjectNode) mapper.readTree(entries.get("backup.json"));
		ObjectNode modification = (ObjectNode) json.withArray("cars").get(0).withArray("modifications").get(0);
		// 4 is out of the bills, 1 is the refuel
		for (int bill : new int[] {
				4, 1
		})
		{
			modification.putArray("bills").add(bill);
			entries.put("backup.json", mapper.writeValueAsBytes(json));

			restore(zip(entries)).statusCode(400)
					.body("", contains(
							"A modification of the backup links an entry that isn't in it or can't be linked!"));
		}
		given().when().get("/api/car/my").then().body("size()", is(0));
	}

	@Test
	void shouldRestoreTheOneBillOfAPrerelease() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		ObjectNode json = (ObjectNode) mapper.readTree(entries.get("backup.json"));
		ObjectNode modification = (ObjectNode) json.withArray("cars").get(0).withArray("modifications").get(0);
		modification.remove("bills");
		modification.put("bill", 0);
		entries.put("backup.json", mapper.writeValueAsBytes(json));

		restore(zip(entries)).statusCode(200);
		long restored = given().when().get("/api/car/my").then().extract().jsonPath().getLong("[0].id");
		given().when().get("/api/modification/{carId}", restored).then()
				.body("[0].bills.description", contains("Oil change"));
	}

	@Test
	void shouldRestoreNothingWithADocumentWithoutKind() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		ObjectNode json = (ObjectNode) mapper.readTree(entries.get("backup.json"));
		((ObjectNode) json.withArray("cars").get(0).withArray("modifications").get(0).withArray("documents").get(0))
				.remove("kind");
		entries.put("backup.json", mapper.writeValueAsBytes(json));

		restore(zip(entries)).statusCode(400).body("", contains("The kind of the document must be set!"));
		given().when().get("/api/car/my").then().body("size()", is(0));
	}

	@Test
	void shouldRestoreNothingWithTwoOpenTrips() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		ObjectNode json = (ObjectNode) mapper.readTree(entries.get("backup.json"));
		var trips = ((ObjectNode) json.withArray("cars").get(0)).withArray("trips");
		trips.add(trips.get(1).deepCopy());
		entries.put("backup.json", mapper.writeValueAsBytes(json));

		restore(zip(entries)).statusCode(400)
				.body("[0]", is("A car of the backup has more than one open trip!"));
		given().when().get("/api/car/my").then().body("size()", is(0));
	}

	@Test
	void shouldRestoreADeletedTripWithItsDeletion() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		ObjectNode json = (ObjectNode) mapper.readTree(entries.get("backup.json"));
		var trips = ((ObjectNode) json.withArray("cars").get(0)).withArray("trips");
		// a second open trip, deleted: it doesn't block the open one
		ObjectNode deleted = trips.get(1).deepCopy();
		deleted.put("deletedAt", "2025-03-05T18:00:00Z").put("deletedBy", "peter").put("deleteReason", "Twice");
		trips.add(deleted);
		entries.put("backup.json", mapper.writeValueAsBytes(json));

		restore(zip(entries)).statusCode(200).body("trips", is(3));
		long restored = given().when().get("/api/car/my").then().extract().jsonPath().getLong("[0].id");
		given().when().get("/api/trip/{carId}", restored).then()
				.body("data.deleteReason", hasItem("Twice"))
				.body("data.deletedBy", hasItem("peter"));
		given().when().get("/api/trip/{carId}/current", restored).then()
				.body("open.deletedAt", nullValue());
	}

	@Test
	void shouldRestoreNothingWithAnIncompleteDeletion() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		ObjectNode json = (ObjectNode) mapper.readTree(entries.get("backup.json"));
		var trips = ((ObjectNode) json.withArray("cars").get(0)).withArray("trips");
		((ObjectNode) trips.get(0)).put("deletedAt", "2025-03-05T18:00:00Z");
		entries.put("backup.json", mapper.writeValueAsBytes(json));

		restore(zip(entries)).statusCode(400).body("[0]",
				is("A deleted trip of the backup needs the time, the account and the reason of the deletion!"));
	}

	@Test
	void shouldOnlyRestoreIntoAnAccountWithoutCars()
	{
		restore(download()).statusCode(409);
	}

	@Test
	void shouldRejectOtherFilesAndVersions() throws IOException
	{
		byte[] backup = download();
		deleteCar();

		restore("no zip".getBytes(StandardCharsets.UTF_8)).statusCode(400);
		restore(zip(Map.of("other.json", "{}".getBytes(StandardCharsets.UTF_8)))).statusCode(400)
				.body("[0]", is("The file is no backup of CarLedger!"));

		Map<String, byte[]> entries = unzip(backup);
		ObjectNode json = (ObjectNode) mapper.readTree(entries.get("backup.json"));
		json.put("version", 2);
		entries.put("backup.json", mapper.writeValueAsBytes(json));
		restore(zip(entries)).statusCode(400)
				.body("[0]", is("Backups of version 2 cannot be restored, only version 1!"));
	}

	@Test
	void shouldRestoreNothingWithMoreCarsThanAllowed() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		ObjectNode json = (ObjectNode) mapper.readTree(entries.get("backup.json"));
		var cars = json.withArray("cars");
		cars.add(cars.get(0).deepCopy());
		entries.put("backup.json", mapper.writeValueAsBytes(json));

		restore(zip(entries)).statusCode(400)
				.body("[0]", containsString("The backup has 2 cars, your account allows 1"));
		given().when().get("/api/car/my").then().body("size()", is(0));
	}

	@Test
	void shouldRejectAZipBomb() throws IOException
	{
		deleteCar();
		// 20 MB of spaces pack into a few KB
		byte[] json = new byte[20 * 1024 * 1024];
		java.util.Arrays.fill(json, (byte) ' ');
		byte[] bomb = zip(Map.of("backup.json", json));
		assertTrue(bomb.length < 100 * 1024);

		restore(bomb).statusCode(400).body("[0]", is("The file backup.json of the backup is too large!"));
	}

	@Test
	void shouldRejectAFileLargerThanAnAttachment() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		// the tests allow 64 KB per file
		String path = entries.keySet().stream().filter(name -> name.startsWith("files/")).findFirst().orElseThrow();
		byte[] large = java.util.Arrays.copyOf(entries.get(path), 100 * 1024);
		entries.put(path, large);

		restore(zip(entries)).statusCode(400).body("[0]", is("The file " + path + " of the backup is too large!"));
		given().when().get("/api/car/my").then().body("size()", is(0));
	}

	@Test
	void shouldRejectAFileUsedTwice() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		ObjectNode json = (ObjectNode) mapper.readTree(entries.get("backup.json"));
		// the picture of the car points to the file of an attachment
		ObjectNode car = (ObjectNode) json.withArray("cars").get(0);
		JsonNode attachment = null;
		for (JsonNode bill : car.withArray("bills"))
		{
			if (bill.path("attachments").size() > 0)
			{
				attachment = bill.path("attachments").get(0);
			}
		}
		car.set("picture", attachment.deepCopy());
		entries.put("backup.json", mapper.writeValueAsBytes(json));

		restore(zip(entries)).statusCode(400).body("[0]", containsString("is used twice in the backup"));
		given().when().get("/api/car/my").then().body("size()", is(0));
	}

	@Test
	void shouldRestoreNothingWithAMissingFile() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		entries.keySet().removeIf(name -> name.startsWith("files/"));

		restore(zip(entries)).statusCode(400)
				.body("[0]", containsString("is missing in the backup"));
		given().when().get("/api/car/my").then().body("size()", is(0));
		assertTrue(entries.containsKey("backup.json"));
	}
}
