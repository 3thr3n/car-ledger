package de.codeflowwizardry.carledger.tracker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
import de.codeflowwizardry.carledger.rest.car.tracker.DeviceInput;
import de.codeflowwizardry.carledger.rest.device.TrackPointInput;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Peter's car sends its points while a matching service ({@link FakeValhalla})
 * is set up: the finished drives are snapped to the roads in the background,
 * the map shows them there
 */
@QuarkusTest
@QuarkusTestResource(value = FakeValhalla.class, restrictToAnnotatedClass = true)
@TestSecurity(user = "peter", roles = "user")
class MapMatcherTest
{
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
	MapMatcher matcher;
	@Inject
	EntityManager entityManager;

	long carId;
	long deviceId;
	/** an hour ago, a finished drive */
	OffsetDateTime start;

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();
		FakeValhalla.down.set(false);
		start = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).minusHours(1);
		AccountEntity account = new AccountEntity();
		account.setUserId("peter");
		account.setMaxCars(1);
		accountRepository.persist(account);
		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName("Car");
		carRepository.persist(car);
		carId = car.getId();
		deviceId = deviceService.create(carId, new DeviceInput("ESP32"), "peter").device().id();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
		FakeValhalla.down.set(false);
	}

	/** a point a second, 10 m to the north from 50°N at the longitude */
	private void drive(OffsetDateTime from, int seconds, double lat, double lon)
	{
		List<TrackPointInput> points = new ArrayList<>();
		for (int i = 0; i < seconds; i++)
		{
			points.add(new TrackPointInput(from.plusSeconds(i), lat + i * 10 * LAT_PER_METER, lon, 36.0, null,
					null));
		}
		QuarkusTransaction.requiringNew().run(() -> trackPointRepository.insert(deviceId, carId, points));
	}

	/** the amount of points of the car per state of the matching */
	private Map<String, Long> states()
	{
		return QuarkusTransaction.requiringNew().call(() -> {
			Map<String, Long> states = new java.util.TreeMap<>();
			for (Object row : entityManager.createNativeQuery(
					"SELECT coalesce(tk_match, '-'), count(*) FROM track_point WHERE tk_car_id = :car GROUP BY 1")
					.setParameter("car", carId).getResultList())
			{
				Object[] columns = (Object[]) row;
				states.put(columns[0].toString(), ((Number) columns[1]).longValue());
			}
			return states;
		});
	}

	private io.restassured.response.ValidatableResponse track()
	{
		return given().pathParam("carId", carId)
				.queryParam("from", start.minusHours(1).toString())
				.queryParam("to", OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1).toString())
				.when().get("/api/track/{carId}").then().statusCode(200);
	}

	@Test
	void shouldShowAFinishedDriveOnTheRoad()
	{
		drive(start, 60, 50.0, 10.0);
		assertEquals(Map.of("P", 60L), states());

		matcher.matchAll();

		assertEquals(Map.of("M", 60L), states());
		// on the road 11 m north, the raw points stay
		track().body("segments[0].points[0][1]", is((float) (50.0 + FakeValhalla.NORTH)));
		assertEquals(50.0, QuarkusTransaction.requiringNew().call(() -> ((Number) entityManager
				.createNativeQuery("SELECT min(tk_lat) FROM track_point WHERE tk_car_id = :car")
				.setParameter("car", carId).getSingleResult()).doubleValue()));
	}

	@Test
	void shouldWaitForADriveStillOnTheRoad()
	{
		drive(start, 60, 50.0, 10.0);
		// the last ones a minute ago, the car may still be driving
		drive(OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).minusMinutes(8), 450, 50.1, 10.0);

		matcher.matchAll();

		assertEquals(Map.of("M", 60L, "P", 450L), states());
	}

	@Test
	void shouldKeepTheGpsWhereTheRoadIsTooFar()
	{
		// a parking lot: 100 m from the road
		drive(start, 30, 50.0, 10.6);
		// no road at all
		drive(start.plusMinutes(10), 30, 61.0, 10.0);

		matcher.matchAll();

		assertEquals(Map.of("U", 60L), states());
		track().body("segments[0].points[0][1]", is(50.0f));
	}

	@Test
	void shouldMatchOnceTheServiceIsBack()
	{
		drive(start, 60, 50.0, 10.0);
		FakeValhalla.down.set(true);

		matcher.matchAll();
		assertEquals(Map.of("P", 60L), states());

		FakeValhalla.down.set(false);
		matcher.matchAll();
		assertEquals(Map.of("M", 60L), states());
	}

	@Test
	void shouldSplitALongDrive()
	{
		int before = FakeValhalla.requests.get();
		// more than the 2000 points of a request
		drive(start.minusHours(1), 2500, 50.0, 10.0);

		matcher.matchAll();

		assertEquals(Map.of("M", 2500L), states());
		assertEquals(2, FakeValhalla.requests.get() - before);
	}
}
