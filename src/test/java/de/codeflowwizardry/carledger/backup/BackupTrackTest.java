package de.codeflowwizardry.carledger.backup;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.DeviceEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.DeviceRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
import de.codeflowwizardry.carledger.rest.car.tracker.DeviceInput;
import de.codeflowwizardry.carledger.rest.device.TrackPointInput;
import de.codeflowwizardry.carledger.tracker.DeviceService;
import de.codeflowwizardry.carledger.tracker.DeviceService.AuthenticatedDevice;
import de.codeflowwizardry.carledger.tracker.TrackerStatus;
import de.codeflowwizardry.carledger.tracker.DeviceTokens;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Peter's car with two trackers (one revoked) and their points: in the backup
 * only while the car keeps them forever
 */
@QuarkusTest
@TestSecurity(user = "peter", roles = "user")
class BackupTrackTest
{
	private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-20T08:00:00Z");

	@Inject
	AccountRepository accountRepository;
	@Inject
	CarRepository carRepository;
	@Inject
	DeviceRepository deviceRepository;
	@Inject
	DeviceService deviceService;
	@Inject
	TrackPointRepository trackPointRepository;
	@Inject
	ObjectMapper mapper;
	@Inject
	EntityManager em;

	long carId;
	String token;

	@BeforeEach
	void setup()
	{
		carId = QuarkusTransaction.requiringNew().call(() -> {
			accountRepository.deleteAll();
			AccountEntity account = new AccountEntity();
			account.setUserId("peter");
			account.setMaxCars(1);
			accountRepository.persist(account);
			AccountEntity paul = new AccountEntity();
			paul.setUserId("paul");
			paul.setMaxCars(1);
			accountRepository.persist(paul);
			return car(account, "Golf").getId();
		});
		var created = deviceService.create(carId, new DeviceInput("Glovebox"), "peter");
		token = created.token();
		deviceService.status(new AuthenticatedDevice(created.device().id(), carId), new TrackerStatus(1, "v1.1.0",
				null, null, false, "poweron", "ok", 200, true, "Home", -60, List.of("sd")));
		long old = deviceService.create(carId, new DeviceInput("Old one"), "peter").device().id();
		deviceService.revoke(carId, old, "peter");

		QuarkusTransaction.requiringNew().run(() -> {
			trackPointRepository.insert(created.device().id(), carId, List.of(
					new TrackPointInput(START, 50.1, 10.2, 42.5, 250.0, 1.2, 0.123, 0.0, -0.456, "imu"),
					new TrackPointInput(START.plusSeconds(1), 50.1000123, 10.2000149, null, null, null)));
			trackPointRepository.insert(old, carId,
					List.of(new TrackPointInput(START.minusDays(1), -33.9, 151.2, 0.0, -2.5, 0.8)));
		});
	}

	private CarEntity car(AccountEntity account, String name)
	{
		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName(name);
		carRepository.persist(car);
		return car;
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	private byte[] download()
	{
		return given().when().get("/api/user/me/backup").then().statusCode(200).extract().asByteArray();
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

	private ValidatableResponse restore(byte[] zip)
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

	private void setRetention(Integer days)
	{
		given().contentType(ContentType.JSON).body(days == null ? "{\"days\": null}" : "{\"days\": " + days + "}")
				.when().put("/api/track/{carId}/retention", carId).then().statusCode(204);
	}

	private long restoredCar()
	{
		return given().when().get("/api/car/my").then().statusCode(200).extract().jsonPath().getLong("[0].id");
	}

	private long points(long car)
	{
		return QuarkusTransaction.requiringNew().call(() -> ((Number) em
				.createNativeQuery("select count(*) from track_point where tk_car_id = " + car).getSingleResult())
				.longValue());
	}

	private static String track(Map<String, byte[]> entries, JsonNode car)
	{
		return new String(entries.get(car.get("track").get("path").asText()), StandardCharsets.UTF_8);
	}

	@Test
	void shouldWriteTheTrackersAndTheirPoints() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());

		JsonNode car = mapper.readTree(entries.get("backup.json")).get("cars").get(0);
		assertTrue(car.get("trackRetentionDays").isNull());
		JsonNode trackers = car.get("trackers");
		assertEquals(2, trackers.size());
		assertEquals("Glovebox", trackers.get(0).get("name").asText());
		assertEquals(DeviceTokens.hash(token), trackers.get(0).get("tokenHash").asText());
		assertTrue(trackers.get(0).get("revokedAt").isNull());
		assertTrue(!trackers.get(1).get("revokedAt").isNull());
		// the token itself never
		assertTrue(!new String(entries.get("backup.json"), StandardCharsets.UTF_8).contains(token));

		assertEquals("""
				tracker,ts,lat,lon,speed,alt,hdop,accel,brake,lateral,gsource
				1,1792396800,-33.9,151.2,0,-2.5,0.8,,,,
				0,1792483200,50.1,10.2,42.5,250,1.2,0.12,0,-0.46,imu
				0,1792483201,50.100012,10.200015,,,,,,,
				""", track(entries, car));
	}

	@Test
	void shouldRestoreTheTrackersThatKeepSending()
	{
		byte[] backup = download();
		deleteCar();

		restore(backup).statusCode(200).body("trackers", is(2)).body("trackPoints", is(3));

		long restored = restoredCar();
		assertEquals(3, points(restored));
		// the token of the backup still works, the revoked tracker stays revoked
		assertEquals(restored, deviceService.authenticate(token).orElseThrow().carId());
		given().when().get("/api/tracker/{carId}", restored).then()
				.body("name", org.hamcrest.Matchers.contains("Old one", "Glovebox"))
				.body("[0].revokedAt", org.hamcrest.Matchers.notNullValue())
				.body("[1].revokedAt", nullValue());
		given().queryParam("from", START.toString()).queryParam("to", START.plusHours(1).toString())
				.when().get("/api/track/{carId}", restored).then().body("points", is(2));
	}

	@Test
	void shouldLeaveOutThePointsOfACarThatKeepsThemForAWhile() throws IOException
	{
		setRetention(30);
		Map<String, byte[]> entries = unzip(download());

		JsonNode car = mapper.readTree(entries.get("backup.json")).get("cars").get(0);
		assertEquals(30, car.get("trackRetentionDays").asInt());
		assertEquals(2, car.get("trackers").size());
		assertTrue(car.get("track").isNull());
		assertTrue(entries.keySet().stream().noneMatch(name -> name.startsWith("tracks/")));

		deleteCar();
		restore(zip(entries)).statusCode(200).body("trackers", is(2)).body("trackPoints", is(0));
		given().when().get("/api/car/my").then().body("[0].trackRetentionDays", is(30));
	}

	@Test
	void shouldRestoreTheTrackEvenOfACarWithARetention() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		String json = new String(entries.get("backup.json"), StandardCharsets.UTF_8)
				.replace("\"trackRetentionDays\" : null", "\"trackRetentionDays\" : 7");
		entries.put("backup.json", json.getBytes(StandardCharsets.UTF_8));

		// the file decides, the next cleanup the retention
		restore(zip(entries)).statusCode(200).body("trackPoints", is(3));
		given().when().get("/api/car/my").then().body("[0].trackRetentionDays", is(7));
	}

	@Test
	void shouldIssueANewTokenWhenTheHashIsTaken()
	{
		byte[] backup = download();
		deleteCar();
		// e.g. the backup of another account on the same server
		QuarkusTransaction.requiringNew().run(() -> deviceRepository.persist(new DeviceEntity(
				car(accountRepository.findByIdentifier("paul"), "Pauls car"), "Copy", DeviceTokens.hash(token))));

		restore(backup).statusCode(200).body("trackers", is(2));

		long restored = restoredCar();
		assertNotEquals(restored, deviceService.authenticate(token).orElseThrow().carId());
		assertEquals(3, points(restored));
	}

	@Test
	void shouldRestoreTheIsoTimesOfTheDevBuilds() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		JsonNode car = mapper.readTree(entries.get("backup.json")).get("cars").get(0);
		entries.put(car.get("track").get("path").asText(), """
				tracker,ts,lat,lon,speed,alt,hdop
				1,2026-10-19T08:00Z,-33.9,151.2,0.0,-2.5,0.8
				0,2026-10-20T08:00:00.5+02:00,50.1,10.2,42.5,250.0,1.2
				""".getBytes(StandardCharsets.UTF_8));

		restore(zip(entries)).statusCode(200).body("trackPoints", is(2));
		given().queryParam("from", START.minusHours(3).toString()).queryParam("to", START.toString())
				.when().get("/api/track/{carId}", restoredCar()).then().body("points", is(1))
				.body("segments[0][0].ts", is("2026-10-20T06:00:00.5Z"));
	}

	@Test
	void shouldSkipInvalidPointsLikeTheIngest() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		JsonNode car = mapper.readTree(entries.get("backup.json")).get("cars").get(0);
		String path = car.get("track").get("path").asText();
		entries.put(path,
				(track(entries, car)
						+ "0,2019-12-31T23:59:59Z,50.0,10.0,,,,,,,\n0,2026-10-21T08:00Z,91.0,10.0,,,,,,,\n")
						.getBytes(StandardCharsets.UTF_8));

		restore(zip(entries)).statusCode(200).body("trackPoints", is(3));
	}

	@Test
	void shouldRestoreNothingWithAnUnreadableTrack() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		JsonNode car = mapper.readTree(entries.get("backup.json")).get("cars").get(0);
		String path = car.get("track").get("path").asText();

		for (String content : List.of("ts,lat\n", track(entries, car) + "5,2026-10-21T08:00Z,50.0,10.0,,,,,,,\n",
				track(entries, car) + "0,yesterday,50.0,10.0,,,,,,,\n",
				track(entries, car) + "0,2026-10-21T08:00Z,50.0\n"))
		{
			entries.put(path, content.getBytes(StandardCharsets.UTF_8));
			restore(zip(entries)).statusCode(400);
			given().when().get("/api/car/my").then().body("size()", is(0));
		}

		entries.remove(path);
		restore(zip(entries)).statusCode(400);
		given().when().get("/api/car/my").then().body("size()", is(0));
	}

	/** accel, brake, lateral and gsource of the restored points, by time */
	private List<String> gForces(long car)
	{
		return QuarkusTransaction.requiringNew().call(() -> em
				.createNativeQuery("select concat_ws('|', tk_accel, tk_brake, tk_lateral, tk_gsource) from track_point"
						+ " where tk_car_id = " + car + " order by tk_ts", String.class)
				.getResultList());
	}

	@Test
	void shouldRestoreTheGForces()
	{
		byte[] backup = download();
		deleteCar();

		restore(backup).statusCode(200).body("trackPoints", is(3));
		assertEquals(List.of("", "0.12|0|-0.46|imu", ""), gForces(restoredCar()));
	}

	@Test
	void shouldRestoreABackupBeforeTheGForces() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		JsonNode car = mapper.readTree(entries.get("backup.json")).get("cars").get(0);
		entries.put(car.get("track").get("path").asText(), """
				tracker,ts,lat,lon,speed,alt,hdop
				1,1792396800,-33.9,151.2,0,-2.5,0.8
				0,1792483200,50.1,10.2,42.5,250,1.2
				""".getBytes(StandardCharsets.UTF_8));

		restore(zip(entries)).statusCode(200).body("trackPoints", is(2));
		assertEquals(List.of("", ""), gForces(restoredCar()));
	}

	@Test
	void shouldLeaveOutImplausibleGForcesOfABackup() throws IOException
	{
		Map<String, byte[]> entries = unzip(download());
		deleteCar();
		JsonNode car = mapper.readTree(entries.get("backup.json")).get("cars").get(0);
		entries.put(car.get("track").get("path").asText(), """
				tracker,ts,lat,lon,speed,alt,hdop,accel,brake,lateral,gsource
				0,1792483200,50.1,10.2,42.5,250,1.2,2.5,-0.1,-1.9,sonar
				""".getBytes(StandardCharsets.UTF_8));

		restore(zip(entries)).statusCode(200).body("trackPoints", is(1));
		assertEquals(List.of("-1.9|unknown"), gForces(restoredCar()));
	}

	@Test
	void shouldRestoreTheStatusOfTheTrackers()
	{
		byte[] backup = download();
		deleteCar();

		restore(backup).statusCode(200);
		given().when().get("/api/tracker/{carId}", restoredCar()).then()
				.body("[1].name", is("Glovebox"))
				.body("[1].status.firmware", is("v1.1.0"))
				.body("[1].status.parts", org.hamcrest.Matchers.contains("sd"))
				.body("[1].statusAt", org.hamcrest.Matchers.notNullValue())
				.body("[0].status", nullValue());
	}

	@Test
	void shouldBackUpACarWithoutTrackersAsBefore() throws IOException
	{
		QuarkusTransaction.requiringNew().run(() -> deviceRepository.delete("car.id", carId));
		Map<String, byte[]> entries = unzip(download());

		JsonNode car = mapper.readTree(entries.get("backup.json")).get("cars").get(0);
		assertEquals(0, car.get("trackers").size());
		assertNull(car.get("track").textValue());
	}
}
