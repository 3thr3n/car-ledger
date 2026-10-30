package de.codeflowwizardry.carledger.rest.car.track;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.TripService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.TripType;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
import de.codeflowwizardry.carledger.rest.car.trip.TripPastInput;
import de.codeflowwizardry.carledger.rest.car.tracker.DeviceInput;
import de.codeflowwizardry.carledger.rest.device.TrackPointInput;
import de.codeflowwizardry.carledger.tracker.DeviceService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestHTTPEndpoint(TrackResource.class)
@TestSecurity(user = "peter", roles = {
		"user"
})
class TrackResourceTest
{
	private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-20T08:00:00Z");
	/** degrees of latitude per meter */
	private static final double LAT_PER_METER = 1 / 111_195.0;

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	DeviceService deviceService;

	@Inject
	TrackPointRepository trackPointRepository;

	@Inject
	EntityManager entityManager;

	@Inject
	TripService tripService;

	long carId;
	long otherCarId;
	long deviceId;

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();
		carId = car(account("peter"), "Peters car");
		otherCarId = car(account("paul"), "Pauls car");
		deviceId = deviceService.create(carId, new DeviceInput("ESP32"), "peter").device().id();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	private AccountEntity account(String name)
	{
		AccountEntity account = new AccountEntity();
		account.setUserId(name);
		account.setMaxCars(2);
		accountRepository.persist(account);
		return account;
	}

	private long car(AccountEntity account, String name)
	{
		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName(name);
		car.setTripLog(true);
		carRepository.persist(car);
		return car.getId();
	}

	/**
	 * Points every second from the second <code>start</code> on, each 10 m further
	 * north, with a zigzag of 3 m to the east: within the tolerance of the
	 * simplification, but part of the distance
	 */
	private void drive(int start, int count, double speed)
	{
		drive(start, count, speed, 0);
	}

	/**
	 * @param later
	 *            seconds the points are sent later, on the same road
	 */
	private void drive(int start, int count, double speed, int later)
	{
		List<TrackPointInput> points = new ArrayList<>();
		for (int i = 0; i < count; i++)
		{
			int second = start + i;
			double east = i % 2 == 0 ? 0 : 3;
			points.add(new TrackPointInput(START.plusSeconds(later + second), 50 + second * 10 * LAT_PER_METER,
					10 + east * LAT_PER_METER / Math.cos(Math.toRadians(50)), speed, null, null));
		}
		QuarkusTransaction.requiringNew().run(() -> trackPointRepository.insert(deviceId, carId, points));
	}

	@Test
	void shouldSimplifyTheTrackCompact()
	{
		// 1 km straight to the north at 36 km/h: its two ends are enough
		line(0, 0, 1000);

		given()
				.pathParam("carId", carId)
				.queryParam("from", START.toString())
				.queryParam("to", START.plusHours(1).toString())
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("points", is(101))
				.body("segments", hasSize(1))
				.body("segments[0].start", is("2026-10-20T08:00:00Z"))
				// lon, lat, seconds since the start, km/h
				.body("segments[0].points", hasSize(2))
				.body("segments[0].points[0]", contains(10.0f, 50.0f, 0.0f, 36.0f))
				.body("segments[0].points[1][2]", is(100.0f));
	}

	/**
	 * 1 km straight to the north at 36 km/h, with calm g-forces of the source,
	 * braking at second 50 and a curve to the left at second 30
	 */
	private void lineWithGForces(String source)
	{
		List<TrackPointInput> points = new ArrayList<>();
		for (int second = 0; second <= 100; second++)
		{
			points.add(new TrackPointInput(START.plusSeconds(second), 50 + second * 10 * LAT_PER_METER, 10.0, 36.0,
					null, null, 0.05, second == 50 ? 0.62 : 0.0, second == 30 ? -0.4 : 0.1, source));
		}
		QuarkusTransaction.requiringNew().run(() -> trackPointRepository.insert(deviceId, carId, points));
	}

	@Test
	void shouldKeepThePeaksOfTheGForcesOfThePointsLeftOut()
	{
		lineWithGForces("imu");

		// only the ends are kept, the last one carries the peaks of the line to it
		range().queryParam("gforces", true).when().get().then()
				.statusCode(200)
				.body("segments[0].points", hasSize(2))
				.body("segments[0].points[0]", contains(10.0f, 50.0f, 0.0f, 36.0f, 0.05f, 0.0f, 0.1f, 1.0f))
				.body("segments[0].points[1]", contains(10.0f, 50.00899f, 100.0f, 36.0f, 0.05f, 0.62f, -0.4f, 1.0f));
	}

	@Test
	void shouldSendTheGForcesOnlyWhenAsked()
	{
		lineWithGForces("gps");

		range().when().get().then().body("segments[0].points[1]", hasSize(4));
		range().queryParam("gforces", true).when().get().then()
				.body("segments[0].points[1][7]", is(0.0f));
	}

	@Test
	void shouldSendNoGForcesOfAnOlderFirmware()
	{
		line(0, 0, 1000);

		range().queryParam("gforces", true).when().get().then()
				.body("segments[0].points[1]", contains(10.0f, 50.00899f, 100.0f, 36.0f, null, null, null, null));
	}

	@Test
	void shouldNotWaitForTheMapMatchingWithoutAService()
	{
		line(0, 0, 100);

		// none of them in the index of the waiting points
		assertEquals(0, QuarkusTransaction.requiringNew().call(() -> ((Number) entityManager
				.createNativeQuery("SELECT count(*) FROM track_point WHERE tk_match IS NOT NULL")
				.getSingleResult()).intValue()));
	}

	@Test
	void shouldKeepWhatIsMoreThanAMeterOff()
	{
		// the zigzag of 3 m is more than the tolerance of 1 m: the map simplifies by
		// its zoom
		drive(0, 101, 50);

		range().when().get().then().body("segments[0].points", hasSize(101));
	}

	@Test
	void shouldCompressTheTrack()
	{
		drive(0, 101, 50);

		range().header("Accept-Encoding", "gzip").when().get().then()
				.statusCode(200)
				.header("Content-Encoding", "gzip");
		range().header("Accept-Encoding", "gzip").when().get("frequency").then()
				.header("Content-Encoding", "gzip");
		// not the other answers, e.g. of a new token
		given().basePath("/").header("Accept-Encoding", "gzip").when().get("api/tracker/" + carId).then()
				.statusCode(200)
				.header("Content-Encoding", nullValue());
	}

	@Test
	void shouldMeasureTheDistanceOfTheRawPoints()
	{
		drive(0, 101, 50);

		// 1000 m to the north, the zigzag adds about 4.5 m per point: sqrt(10² + 3²)
		given()
				.pathParam("carId", carId)
				.queryParam("from", START.toString())
				.queryParam("to", START.plusHours(1).toString())
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("distance", is(1.04f));
	}

	@Test
	void shouldSplitAtGaps()
	{
		drive(0, 11, 50);
		// 5 minutes are no gap yet
		drive(10 + 300, 11, 50);
		drive(320 + 301, 11, 50);

		given()
				.pathParam("carId", carId)
				.queryParam("from", START.toString())
				.queryParam("to", START.plusHours(1).toString())
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("points", is(33))
				.body("segments", hasSize(2))
				.body("segments[0].start", is("2026-10-20T08:00:00Z"))
				.body("segments[0].points[-1][2]", is(320.0f))
				.body("segments[1].start", is("2026-10-20T08:10:21Z"))
				// first segment: 2 x 104.4 m and the 3 km during the 5 minutes, second: 104.4
				// m; not the 3 km of the gap
				.body("distance", is(3.31f));
	}

	@Test
	void shouldOnlyReturnThePointsOfTheRange()
	{
		drive(0, 11, 50);

		given()
				.pathParam("carId", carId)
				.queryParam("from", START.plusSeconds(5).toString())
				.queryParam("to", START.plusSeconds(10).toString())
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("points", is(5))
				.body("segments[0].start", is("2026-10-20T08:00:05Z"))
				.body("segments[0].points[-1][2]", is(4.0f));
	}

	@Test
	void shouldReturnAnEmptyTrack()
	{
		given()
				.pathParam("carId", carId)
				.queryParam("from", START.toString())
				.queryParam("to", START.plusDays(31).toString())
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("points", is(0))
				.body("segments", empty())
				.body("distance", is(0.0f));
	}

	@Test
	void shouldRejectAnInvalidRange()
	{
		given()
				.pathParam("carId", carId)
				.queryParam("from", START.toString())
				.queryParam("to", START.plusDays(31).plusSeconds(1).toString())
				.when()
				.get()
				.then()
				.statusCode(400)
				.body("", contains("The range must not be longer than 31 days!"));

		given()
				.pathParam("carId", carId)
				.queryParam("from", START.toString())
				.queryParam("to", START.toString())
				.when()
				.get()
				.then()
				.statusCode(400)
				.body("", contains("To must be after from!"));

		given()
				.pathParam("carId", carId)
				.queryParam("from", START.toString())
				.when()
				.get()
				.then()
				.statusCode(400)
				.body("", contains("From and to must be set!"));
	}

	@Test
	void shouldNotReturnTheTrackOfAnotherUser()
	{
		given()
				.pathParam("carId", otherCarId)
				.queryParam("from", START.toString())
				.queryParam("to", START.plusHours(1).toString())
				.when()
				.get()
				.then()
				.statusCode(404);
	}

	@Test
	void shouldCountThePasses()
	{
		drive(0, 101, 50);
		// the same road an hour later
		drive(0, 101, 50, 3600);

		List<Double> lats = given()
				.pathParam("carId", carId)
				.queryParam("from", START.toString())
				.queryParam("to", START.plusHours(2).toString())
				.when()
				.get("frequency")
				.then()
				.statusCode(200)
				.body("maxPasses", is(2))
				.body("lines.passes", everyItem(is(2)))
				// the street once, for both drives
				.body("lines", hasSize(1))
				.body("lines[0].last", is("2026-10-20T09:01:40Z"))
				.extract().jsonPath().getList("lines[0].coordinates.collect { it[1] }", Double.class);
		// from one end of the 1000 m to the other, within a cell of 8 m
		assertEquals(50, lats.stream().mapToDouble(Double::doubleValue).min().orElseThrow(), 8 * LAT_PER_METER);
		assertEquals(50 + 1000 * LAT_PER_METER, lats.stream().mapToDouble(Double::doubleValue).max().orElseThrow(),
				8 * LAT_PER_METER);
	}

	@Test
	void shouldValidateTheRangeOfTheFrequency()
	{
		given()
				.pathParam("carId", carId)
				.queryParam("from", START.toString())
				.queryParam("to", START.plusDays(32).toString())
				.when()
				.get("frequency")
				.then()
				.statusCode(400);
	}

	@Test
	void shouldNotReturnTheFrequencyOfAnotherUser()
	{
		given()
				.pathParam("carId", otherCarId)
				.queryParam("from", START.toString())
				.queryParam("to", START.plusHours(1).toString())
				.when()
				.get("frequency")
				.then()
				.statusCode(404);
	}

	/** requests to the trips of the car, not to the track of TrackResource */
	private static RequestSpecification trips(long car)
	{
		return given().basePath("/api/trip/{carId}").pathParam("carId", car);
	}

	private UUID pastTrip(OffsetDateTime start, OffsetDateTime end)
	{
		UUID id = UUID.randomUUID();
		Map<String, Object> body = new HashMap<>();
		body.put("type", TripType.PRIVATE);
		body.put("startOdometer", 100);
		body.put("startedAt", start.toString());
		body.put("endOdometer", 101);
		body.put("endedAt", end.toString());
		trips(carId)
				.contentType(ContentType.JSON)
				.body(body)
				.when()
				.put("{id}/past", id)
				.then()
				.statusCode(201);
		return id;
	}

	private ValidatableResponse tripTrack(UUID id)
	{
		return trips(carId)
				.when()
				.get("{id}/track", id)
				.then();
	}

	@Test
	void shouldReturnTheTrackDuringATrip()
	{
		drive(0, 101, 50);
		UUID id = pastTrip(START.plusSeconds(10), START.plusSeconds(50));

		// the end of the trip is part of it
		tripTrack(id)
				.statusCode(200)
				.body("points", is(41))
				.body("segments[0].start", is("2026-10-20T08:00:10Z"))
				.body("segments[0].points[-1][2]", is(40.0f))
				.body("distance", is(0.42f));
	}

	@Test
	void shouldReturnTheTrackOfAnOpenTripUntilNow()
	{
		drive(0, 101, 50);
		UUID id = UUID.randomUUID();
		trips(carId)
				.contentType(ContentType.JSON)
				.body(Map.of("type", TripType.PRIVATE, "startOdometer", 100, "startedAt",
						START.plusSeconds(60).toString()))
				.when()
				.put("{id}", id)
				.then()
				.statusCode(201);

		tripTrack(id).statusCode(200).body("points", is(41));
	}

	@Test
	void shouldReturnTheTrackOfADeletedTrip()
	{
		drive(0, 101, 50);
		UUID id = pastTrip(START.plusSeconds(10), START.plusSeconds(50));
		trips(carId)
				.contentType(ContentType.JSON)
				.body(Map.of("reason", "Wrong car"))
				.when()
				.post("{id}/delete", id)
				.then()
				.statusCode(200);

		tripTrack(id).statusCode(200).body("points", is(41));
	}

	@Test
	void shouldReturnAnEmptyTrackOfATripWithoutPoints()
	{
		UUID id = pastTrip(START.plusSeconds(10), START.plusSeconds(50));

		tripTrack(id).statusCode(200).body("points", is(0)).body("segments", empty());
	}

	@Test
	void shouldNotReturnTheTrackOfATripOfAnotherCar()
	{
		UUID id = pastTrip(START.plusSeconds(10), START.plusSeconds(50));

		trips(otherCarId)
				.when()
				.get("{id}/track", id)
				.then()
				.statusCode(404);
	}

	private Integer retention()
	{
		return QuarkusTransaction.requiringNew()
				.call(() -> carRepository.findById(carId, "peter", Action.READ).getTrackRetentionDays());
	}

	@Test
	void shouldSetTheRetention()
	{
		given().contentType(ContentType.JSON).pathParam("carId", carId).body(Map.of("days", 30))
				.when().put("retention").then().statusCode(204);
		assertEquals(30, retention());

		// forever
		given().contentType(ContentType.JSON).pathParam("carId", carId).body("{\"days\": null}")
				.when().put("retention").then().statusCode(204);
		assertNull(retention());
	}

	@Test
	void shouldValidateTheRetention()
	{
		for (int days : new int[] {
				0, -1, 3651
		})
		{
			given().contentType(ContentType.JSON).pathParam("carId", carId).body(Map.of("days", days))
					.when().put("retention").then().statusCode(400)
					.body("", contains("The track points must be kept for 1 to 3650 days, or forever!"));
		}
		given().contentType(ContentType.JSON).pathParam("carId", carId).body(Map.of("days", 3650))
				.when().put("retention").then().statusCode(204);
	}

	@Test
	void shouldDeleteAllTracksAndKeepTheTrackers()
	{
		drive(0, 10, 50);

		given().pathParam("carId", carId).when().delete().then().statusCode(200).body("points", is(10));

		given().pathParam("carId", carId).queryParam("from", START.toString())
				.queryParam("to", START.plusHours(1).toString()).when().get().then().body("points", is(0));
		assertEquals(1, deviceService.list(carId, "peter").size());
	}

	@Test
	void shouldNotChangeTheTracksOfAnotherCar()
	{
		given().contentType(ContentType.JSON).pathParam("carId", otherCarId).body(Map.of("days", 30))
				.when().put("retention").then().statusCode(404);
		given().pathParam("carId", otherCarId).when().delete().then().statusCode(404);
	}

	@Test
	void shouldDeleteOnlyThePointsOfThePeriod()
	{
		drive(0, 10, 50);
		long otherDevice = deviceService.create(otherCarId, new DeviceInput("ESP32"), "paul").device().id();
		QuarkusTransaction.requiringNew().run(() -> trackPointRepository.insert(otherDevice, otherCarId,
				List.of(new TrackPointInput(START.plusSeconds(5), 50.0, 10.0, null, null, null))));

		// from inclusive, to exclusive: seconds 3 to 6
		given().pathParam("carId", carId)
				.queryParam("from", START.plusSeconds(3).toString())
				.queryParam("to", START.plusSeconds(7).toString())
				.when().delete().then().statusCode(200).body("points", is(4));

		given().pathParam("carId", carId).queryParam("from", START.toString())
				.queryParam("to", START.plusHours(1).toString()).when().get().then().body("points", is(6));
		assertEquals(1, QuarkusTransaction.requiringNew().call(() -> ((Number) entityManager
				.createNativeQuery("select count(*) from track_point where tk_car_id = " + otherCarId)
				.getSingleResult()).intValue()));
	}

	@Test
	void shouldValidateThePeriodToDelete()
	{
		drive(0, 10, 50);

		given().pathParam("carId", carId).queryParam("from", START.toString())
				.when().delete().then().statusCode(400)
				.body("", contains("From and to must be set both, or none for all points!"));
		given().pathParam("carId", carId).queryParam("to", START.toString())
				.when().delete().then().statusCode(400);
		given().pathParam("carId", carId).queryParam("from", START.toString()).queryParam("to", START.toString())
				.when().delete().then().statusCode(400)
				.body("", contains("To must be after from!"));

		// longer than a query may be, nothing to fear in deleting
		given().pathParam("carId", carId).queryParam("from", START.minusYears(1).toString())
				.queryParam("to", START.plusYears(1).toString())
				.when().delete().then().statusCode(200).body("points", is(10));
	}

	@Test
	void shouldNotDeleteThePeriodOfAnotherCar()
	{
		given().pathParam("carId", otherCarId).queryParam("from", START.toString())
				.queryParam("to", START.plusHours(1).toString())
				.when().delete().then().statusCode(404);
	}

	/** a point every second, 10 m per second to the north or the south */
	private void line(int start, double fromNorth, double toNorth)
	{
		List<TrackPointInput> points = new ArrayList<>();
		double step = fromNorth < toNorth ? 10 : -10;
		int second = start;
		for (double north = fromNorth; step > 0 ? north <= toNorth : north >= toNorth; north += step)
		{
			points.add(new TrackPointInput(START.plusSeconds(second++), 50 + north * LAT_PER_METER, 10.0, 36.0,
					null, null));
		}
		QuarkusTransaction.requiringNew().run(() -> trackPointRepository.insert(deviceId, carId, points));
	}

	/** parked with the tracker on: a point every 10 seconds */
	private void stand(int start, int seconds, double north)
	{
		List<TrackPointInput> points = new ArrayList<>();
		for (int second = start; second < start + seconds; second += 10)
		{
			points.add(new TrackPointInput(START.plusSeconds(second), 50 + north * LAT_PER_METER, 10.0, 0.0, null,
					null));
		}
		QuarkusTransaction.requiringNew().run(() -> trackPointRepository.insert(deviceId, carId, points));
	}

	/** from home to work, 7 minutes there with the tracker on, and back */
	private void commute()
	{
		line(0, 0, 1000);
		stand(101, 420, 1000);
		line(530, 1000, 0);
		stand(631, 420, 0);
	}

	private RequestSpecification range()
	{
		return given().pathParam("carId", carId)
				.queryParam("from", START.toString())
				.queryParam("to", START.plusHours(1).toString());
	}

	/** start/end of the drive of the list, newest first */
	private String drive(int index)
	{
		Map<String, String> drive = range().when().get("drives").then().extract().jsonPath()
				.getMap("[" + index + "]");
		return drive.get("start") + "/" + drive.get("end");
	}

	@Test
	void shouldListTheDrivesWithTheNamedPlaces()
	{
		commute();
		for (Object[] place : new Object[][] {
				{
						0, "Home"
				}, {
						1000, "Work"
				}
		})
		{
			given().pathParam("carId", carId).contentType(ContentType.JSON)
					.body(Map.of("lat", 50 + (int) place[0] * LAT_PER_METER, "lon", 10.0, "name", place[1]))
					.when().put("places").then().statusCode(204);
		}

		range().when().get("drives").then()
				.statusCode(200)
				.body("size()", is(2))
				// the newest first, the standing at work in between isn't part of a drive
				.body("[0].from", is("Work"))
				.body("[0].to", is("Home"))
				.body("[0].end", is("2026-10-20T08:10:30Z"))
				.body("[1].from", is("Home"))
				.body("[1].to", is("Work"))
				// from the last point within the drift of the GPS
				.body("[1].start", is("2026-10-20T08:00:03Z"))
				.body("[1].end", is("2026-10-20T08:01:40Z"))
				.body("[1].distance", is(0.97f));
	}

	@Test
	void shouldMarkTheDrivesWithATrip()
	{
		commute();
		// a trip during the way there, its times a little off the ones of the GPS
		tripService.addPast(carId, UUID.randomUUID(), new TripPastInput(TripType.PRIVATE, 0,
				START.plusSeconds(10), 1, START.plusMinutes(2), null, null, null), "peter");

		range().when().get("drives").then()
				.body("trip", contains(false, true));

		QuarkusTransaction.requiringNew()
				.run(() -> carRepository.findById(carId, "peter", Action.MANAGE).setTripLog(false));
		range().when().get("drives").then()
				.body("trip", contains(false, false));
	}

	@Test
	void shouldListDrivesWithoutNamesAndAtTheEndsOfTheRange()
	{
		// already on the road at the start of the range, still on the road at its end
		line(0, 0, 1000);

		given().pathParam("carId", carId)
				.queryParam("from", START.plusSeconds(10).toString())
				.queryParam("to", START.plusSeconds(90).toString())
				.when().get("drives").then()
				.statusCode(200)
				.body("size()", is(1))
				.body("[0].start", is("2026-10-20T08:00:13Z"))
				.body("[0].end", is("2026-10-20T08:01:26Z"))
				.body("[0].from", nullValue());
	}

	@Test
	void shouldValidateTheRangeOfTheDrives()
	{
		given().pathParam("carId", carId).queryParam("from", START.toString())
				.when().get("drives").then().statusCode(400);
		given().pathParam("carId", otherCarId).queryParam("from", START.toString())
				.queryParam("to", START.plusHours(1).toString())
				.when().get("drives").then().statusCode(404);
	}

	@Test
	void shouldShowOnlyTheSelectedDrives()
	{
		commute();
		String back = drive(0);

		range().queryParam("drive", back).when().get().then()
				.statusCode(200)
				.body("segments.size()", is(1))
				.body("segments[0].start", is(back.split("/")[0]))
				.body("segments[0].points[-1][2]", is((float) Duration
						.between(OffsetDateTime.parse(back.split("/")[0]), OffsetDateTime.parse(back.split("/")[1]))
						.toSeconds()))
				.body("distance", is(0.97f));
		// both: each a segment of its own
		range().queryParam("drive", back).queryParam("drive", drive(1)).when().get().then()
				.body("segments.size()", is(2))
				.body("distance", is(1.94f));
	}

	@Test
	void shouldCountThePassesOfTheSelectedDrives()
	{
		commute();

		range().when().get("frequency").then().body("maxPasses", is(2));
		range().queryParam("drive", drive(1)).when().get("frequency").then().body("maxPasses", is(1));
	}

	@Test
	void shouldRejectAnInvalidSelection()
	{
		range().queryParam("drive", "yesterday").when().get().then()
				.statusCode(400)
				.body("", contains("A drive is start/end, ISO-8601: yesterday"));
		range().queryParam("drive", START.plusMinutes(1) + "/" + START).when().get("frequency").then()
				.statusCode(400);
	}

	@Test
	void shouldDeleteOnlyTheSelectedDrive()
	{
		commute();
		String[] there = drive(1).split("/");
		String[] back = drive(0).split("/");

		// as the map deletes a drive: up to the start of the next one, with the stop
		// after it, else the stops around it would be joined by a straight line
		given().pathParam("carId", carId)
				.queryParam("from", there[0])
				.queryParam("to", back[0])
				.when().delete().then().statusCode(200);

		// the way back stays; it starts a few seconds later, its stop before is gone
		range().when().get("drives").then()
				.body("size()", is(1))
				.body("[0].start", startsWith("2026-10-20T08:08:5"))
				.body("[0].end", startsWith("2026-10-20T08:10:"));
	}
}
