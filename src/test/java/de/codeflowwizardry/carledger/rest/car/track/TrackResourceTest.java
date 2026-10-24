package de.codeflowwizardry.carledger.rest.car.track;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.TripType;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
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
		List<TrackPointInput> points = new ArrayList<>();
		for (int i = 0; i < count; i++)
		{
			int second = start + i;
			double east = i % 2 == 0 ? 0 : 3;
			points.add(new TrackPointInput(START.plusSeconds(second), 50 + second * 10 * LAT_PER_METER,
					10 + east * LAT_PER_METER / Math.cos(Math.toRadians(50)), speed, null, null));
		}
		QuarkusTransaction.requiringNew().run(() -> trackPointRepository.insert(deviceId, carId, points));
	}

	@Test
	void shouldSimplifyTheTrack()
	{
		drive(0, 101, 50);

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
				.body("segments[0]", hasSize(2))
				.body("segments[0][0].ts", is("2026-10-20T08:00:00Z"))
				.body("segments[0][1].ts", is("2026-10-20T08:01:40Z"))
				.body("segments[0][0].speed", is(50.0f))
				.body("segments[0][0].lat", is(50.0f));
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
				.body("segments[0][0].ts", is("2026-10-20T08:00:00Z"))
				.body("segments[0][-1].ts", is("2026-10-20T08:05:20Z"))
				.body("segments[1][0].ts", is("2026-10-20T08:10:21Z"))
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
				.body("segments[0][0].ts", is("2026-10-20T08:00:05Z"))
				.body("segments[0][-1].ts", is("2026-10-20T08:00:09Z"));
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
				.body("segments[0][0].ts", is("2026-10-20T08:00:10Z"))
				.body("segments[0][-1].ts", is("2026-10-20T08:00:50Z"))
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
}
