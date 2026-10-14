package de.codeflowwizardry.carledger.backup;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.codeflowwizardry.carledger.ReminderService;
import de.codeflowwizardry.carledger.attachment.AttachmentService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.BillCategory;
import de.codeflowwizardry.carledger.data.BillInterval;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.ChargingLocation;
import de.codeflowwizardry.carledger.data.EnergyType;
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
import de.codeflowwizardry.carledger.rest.car.miscellaneous.MiscellaneousBillInput;
import de.codeflowwizardry.carledger.rest.car.recurring.RecurringBillInput;
import de.codeflowwizardry.carledger.rest.car.reminder.ReminderInput;
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
	AttachmentService attachmentService;
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
		maintenanceBillFactory.create(new MaintenanceBillInput(BigDecimal.valueOf(119), 19, LocalDate.of(2025, 1, 20),
				41_000, BigDecimal.valueOf(80), BigDecimal.valueOf(39), "Oil change", "Garage"), carId, "peter");
		miscellaneousBillFactory.create(
				new MiscellaneousBillInput(BigDecimal.TEN, 19, LocalDate.of(2025, 3, 1), "Car wash"), carId, "peter");
		recurringBillFactory.create(carId, new RecurringBillInput("Insurance", "Full", BillInterval.MONTHLY,
				BillCategory.INSURANCE, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1), BigDecimal.valueOf(45)),
				"peter");
		reminderService.create(carId, new ReminderInput("Service", "Oil", LocalDate.of(2026, 5, 1), 60_000, 12,
				15_000), "peter");
		attachmentService.setCarPicture(carId, "peter", "golf.png", PNG);
	}

	@Transactional
	long createAccountAndCar()
	{
		accountRepository.deleteAll();
		AccountEntity account = new AccountEntity();
		account.setUserId("peter");
		account.setMaxCars(1);
		account.setCurrency("CHF");
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
		assertEquals(15_000, car.get("reminders").get(0).get("intervalKm").asInt());
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
				.body("reminders", is(1))
				.body("files", is(3));

		long restored = given().when().get("/api/car/my").then().statusCode(200)
				.body("name", contains("Golf GTE"))
				.body("[0].pictureId", notNullValue())
				.body("[0].vin", is("WVWZZZ1KZ6W000001"))
				.body("[0].batteryCapacity", is(13.0F))
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
				.body("data[0].total", is(90.0F));
		given().when().get("/api/reminder/{carId}", restored).then()
				.body("[0].title", is("Service"));
		byte[] picture = given().when().get("/api/car/my/{id}/picture", restored).then().statusCode(200)
				.extract().asByteArray();
		assertArrayEquals(PNG, picture);
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
