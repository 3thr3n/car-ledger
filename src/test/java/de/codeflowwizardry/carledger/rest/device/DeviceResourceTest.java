package de.codeflowwizardry.carledger.rest.device;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.tracker.DeviceCreated;
import de.codeflowwizardry.carledger.rest.car.tracker.DeviceInput;
import de.codeflowwizardry.carledger.tracker.DeviceService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * A GPS tracker sends its points with its token. Redirects are not followed:
 * without a valid login the login page of Keycloak would answer with 200.
 */
@QuarkusTest
class DeviceResourceTest
{
	private static final String POINTS = "/api/device/points";

	private final KeycloakTestClient keycloak = new KeycloakTestClient();

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	DeviceService deviceService;

	@Inject
	EntityManager entityManager;

	long carId;
	long otherCarId;
	DeviceCreated device;

	final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);

	@BeforeEach
	void setup()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			accountRepository.deleteAll();
			carId = car(account("peter"), "Peters car");
			otherCarId = car(account("paul"), "Pauls car");
		});
		device = deviceService.create(carId, new DeviceInput("ESP32"), "peter");
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
		carRepository.persist(car);
		return car.getId();
	}

	private static Map<String, Object> point(OffsetDateTime ts)
	{
		Map<String, Object> point = new HashMap<>();
		point.put("ts", ts.toString());
		point.put("lat", 51.1);
		point.put("lon", 10.2);
		point.put("speed", 42.3);
		point.put("alt", 250.5);
		point.put("hdop", 1.2);
		return point;
	}

	private static Map<String, Object> point(OffsetDateTime ts, String key, Object value)
	{
		Map<String, Object> point = point(ts);
		point.put(key, value);
		return point;
	}

	private ValidatableResponse send(String token, Object body)
	{
		return given()
				.auth().oauth2(token)
				.contentType(ContentType.JSON)
				.body(body)
				.redirects().follow(false)
				.when()
				.post(POINTS)
				.then();
	}

	private long countPoints(long car)
	{
		return QuarkusTransaction.requiringNew().call(() -> ((Number) entityManager
				.createNativeQuery("select count(*) from track_point where tk_car_id = ?1")
				.setParameter(1, car)
				.getSingleResult()).longValue());
	}

	private Object lastSeenAt()
	{
		return QuarkusTransaction.requiringNew().call(() -> entityManager
				.createNativeQuery("select dv_last_seen_at from device where id = ?1")
				.setParameter(1, device.device().id())
				.getSingleResult());
	}

	@Test
	void shouldStoreThePointsForTheCarOfTheTracker()
	{
		send(device.token(), List.of(point(now.minusSeconds(2)), point(now.minusSeconds(1))))
				.statusCode(200)
				.body("accepted", is(2))
				.body("duplicates", is(0))
				.body("rejected", is(0));

		assertEquals(2, countPoints(carId));
		assertEquals(0, countPoints(otherCarId));
	}

	@Test
	void shouldStoreAPointWithoutTheOptionalValues()
	{
		send(device.token(), List.of(Map.of("ts", now.toString(), "lat", -33.9, "lon", 151.2)))
				.statusCode(200)
				.body("accepted", is(1));
	}

	@Test
	void shouldStoreTheGForcesAndLeaveOutImplausibleOnes()
	{
		Map<String, Object> imu = point(now.minusSeconds(3));
		imu.putAll(Map.of("accel", 0.31, "brake", 0, "lateral", -0.42, "gsource", "imu"));
		// a performance car, without a source: a firmware before the IMU, from the GPS
		Map<String, Object> gps = point(now.minusSeconds(2));
		gps.putAll(Map.of("accel", 1.95, "lateral", 2.0));
		// only the implausible values are left out, the point stays
		Map<String, Object> odd = point(now.minusSeconds(1));
		odd.putAll(Map.of("accel", -0.1, "brake", 2.1, "lateral", -0.8, "gsource", "Laser"));

		send(device.token(), List.of(imu, gps, odd, point(now))).statusCode(200).body("accepted", is(4))
				.body("rejected", is(0));

		assertEquals(List.of("0.31|0|-0.42|imu", "1.95|2|gps", "-0.8|unknown", ""),
				QuarkusTransaction.requiringNew().call(() -> entityManager.createNativeQuery(
						"select concat_ws('|', tk_accel, tk_brake, tk_lateral, tk_gsource) from track_point"
								+ " where tk_car_id = ?1 order by tk_ts",
						String.class).setParameter(1, carId).getResultList()));
	}

	@Test
	void shouldDropPointsSentAgain()
	{
		List<Map<String, Object>> points = List.of(point(now.minusSeconds(2)), point(now.minusSeconds(1)));
		send(device.token(), points).statusCode(200);

		send(device.token(), List.of(point(now.minusSeconds(2)), point(now)))
				.statusCode(200)
				.body("accepted", is(1))
				.body("duplicates", is(1))
				.body("rejected", is(0));

		assertEquals(3, countPoints(carId));
	}

	@Test
	void shouldSkipInvalidPoints()
	{
		List<Object> points = new ArrayList<>();
		points.add(point(now));
		points.add(point(now.minusSeconds(1), "lat", 90.5));
		points.add(point(now.minusSeconds(2), "lon", -181));
		points.add(point(now.plusMinutes(6)));
		points.add(point(OffsetDateTime.parse("2019-12-31T23:59:59Z")));
		points.add(point(now.minusSeconds(3), "speed", 401));
		points.add(point(now.minusSeconds(4), "speed", -1));
		points.add(point(now.minusSeconds(5), "hdop", 0));
		points.add(point(now.minusSeconds(6), "lat", null));
		points.add(null);

		send(device.token(), points)
				.statusCode(200)
				.body("accepted", is(1))
				.body("duplicates", is(0))
				.body("rejected", is(9));
	}

	@Test
	void shouldAcceptAClockOfTheTrackerALittleAhead()
	{
		send(device.token(), List.of(point(now.plusMinutes(4))))
				.statusCode(200)
				.body("accepted", is(1));
	}

	@Test
	void shouldRejectAnOversizedBatch()
	{
		List<Map<String, Object>> points = new ArrayList<>();
		for (int i = 0; i < 11; i++)
		{
			points.add(point(now.minusSeconds(i)));
		}

		send(device.token(), points).statusCode(400);
		assertEquals(0, countPoints(carId));
	}

	@Test
	void shouldRejectABodyThatIsNotAnArrayOfPoints()
	{
		send(device.token(), "{\"ts\":").statusCode(400);
		send(device.token(), point(now)).statusCode(400);
		send(device.token(), "null").statusCode(400);
		send(device.token(), List.of(point(now, "lat", "north"))).statusCode(400);
	}

	@Test
	void shouldRejectARevokedTracker()
	{
		deviceService.revoke(carId, device.device().id(), "peter");

		send(device.token(), List.of(point(now))).statusCode(401);
		assertEquals(0, countPoints(carId));
	}

	@Test
	void shouldRejectAnUnknownToken()
	{
		send("cl_" + device.token().substring(4), List.of(point(now))).statusCode(401);
	}

	@Test
	void shouldRejectARequestWithoutToken()
	{
		given()
				.contentType(ContentType.JSON)
				.body(List.of(point(now)))
				.redirects().follow(false)
				.when()
				.post(POINTS)
				.then()
				.statusCode(401)
				.header("WWW-Authenticate", "Bearer");
	}

	@Test
	void shouldRejectTheTokenOfAUser()
	{
		send(keycloak.getAccessToken("bob"), List.of(point(now))).statusCode(401);
	}

	@Test
	void shouldNotOpenTheApiOfTheUsersWithTheTokenOfATracker()
	{
		given()
				.auth().oauth2(device.token())
				.redirects().follow(false)
				.when()
				.get("/api/car/my")
				.then()
				.statusCode(401);

		given()
				.auth().oauth2(device.token())
				.redirects().follow(false)
				.when()
				.get("/api/tracker/{carId}", carId)
				.then()
				.statusCode(401);
	}

	@Test
	void shouldLimitTheRequestsOfATracker()
	{
		for (int i = 0; i < 5; i++)
		{
			send(device.token(), List.of(point(now.minusSeconds(i)))).statusCode(200);
		}

		send(device.token(), List.of(point(now.minusSeconds(10))))
				.statusCode(429)
				.header("Retry-After", notNullValue());

		// another tracker has its own limit
		DeviceCreated other = deviceService.create(carId, new DeviceInput("Second"), "peter");
		send(other.token(), List.of(point(now))).statusCode(200);
	}

	@Test
	void shouldUpdateTheLastContact()
	{
		assertNull(lastSeenAt());

		send(device.token(), List.of(point(now))).statusCode(200);

		assertNotNull(lastSeenAt());
	}

	@Test
	void shouldDeleteTheTrackersAndPointsWithTheCar()
	{
		send(device.token(), List.of(point(now))).statusCode(200);

		carRepository.deleteForUser(carId, "peter");

		assertEquals(0, countPoints(carId));
		send(device.token(), List.of(point(now.minusSeconds(1)))).statusCode(401);
	}
}
