package de.codeflowwizardry.carledger.rest.car.track;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.OffsetDateTime;
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
import de.codeflowwizardry.carledger.tracker.DeviceService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * The places of a car from the stops of its track, and their names
 */
@QuarkusTest
@TestSecurity(user = "peter", roles = "user")
class PlacesTest
{
	private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-01T08:00:00Z");
	/** degrees of latitude per meter */
	private static final double LAT_PER_METER = 1 / 111_195.0;
	/** meters north of home */
	private static final int STATION = 1_500;

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	DeviceService deviceService;

	@Inject
	TrackPointRepository trackPointRepository;

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
		account.setMaxCars(1);
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

	/**
	 * On each day: parked at home, to the fuel station and back, 7 minutes at
	 * each end
	 */
	private void commute(int days)
	{
		for (int day = 0; day < days; day++)
		{
			List<TrackPointInput> points = new ArrayList<>();
			OffsetDateTime time = START.plusDays(day);
			time = stand(points, time, 0);
			time = drive(points, time, 0, STATION);
			time = stand(points, time, STATION);
			time = drive(points, time, STATION, 0);
			stand(points, time, 0);
			QuarkusTransaction.requiringNew().run(() -> trackPointRepository.insert(deviceId, carId, points));
		}
	}

	/** a point every minute for 7 minutes */
	private static OffsetDateTime stand(List<TrackPointInput> points, OffsetDateTime time, int north)
	{
		for (int i = 0; i < 7; i++)
		{
			points.add(point(time, north, 0));
			time = time.plusMinutes(1);
		}
		return time;
	}

	/** 10 m/s, a point every 10 seconds */
	private static OffsetDateTime drive(List<TrackPointInput> points, OffsetDateTime time, int from, int to)
	{
		int step = from < to ? 100 : -100;
		for (int north = from + step; north != to + step; north += step)
		{
			time = time.plusSeconds(10);
			points.add(point(time, north, 36));
		}
		return time;
	}

	private static TrackPointInput point(OffsetDateTime time, int north, double speed)
	{
		return new TrackPointInput(time, 50 + north * LAT_PER_METER, 10.0, speed, null, null);
	}

	private ValidatableResponse places(OffsetDateTime from, OffsetDateTime to)
	{
		return given()
				.queryParam("from", from.toString())
				.queryParam("to", to.toString())
				.get("/api/track/{carId}/places", carId)
				.then();
	}

	private ValidatableResponse name(double north, String name)
	{
		return given()
				.contentType(ContentType.JSON)
				.body(Map.of("lat", 50 + north * LAT_PER_METER, "lon", 10.0, "name", name))
				.put("/api/track/{carId}/places", carId)
				.then();
	}

	@Test
	void shouldFindThePlacesTheCarStopsAtOften()
	{
		commute(3);

		List<Float> lats = places(START.minusHours(1), START.plusDays(3)).statusCode(200)
				.body("size()", is(2))
				.body("visits", contains(3, 3))
				.body("name", everyItem(nullValue()))
				.body("id", everyItem(nullValue()))
				.body("lastVisit", hasItem("2026-10-03T08:09:30Z"))
				.extract().path("lat");
		// home and the fuel station
		assertThat(lats.stream().map(lat -> (lat - 50) / LAT_PER_METER).toList(),
				containsInAnyOrder(closeTo(0, 5), closeTo(STATION, 5)));
	}

	@Test
	void shouldNeedThreeVisits()
	{
		commute(2);

		places(START.minusHours(1), START.plusDays(3)).statusCode(200).body("size()", is(0));
	}

	@Test
	void shouldKeepTheNameOfAPlace()
	{
		commute(3);

		// a bit next to the middle of the stops
		name(STATION + 20, " Fuel station ").statusCode(204);
		places(START.minusHours(1), START.plusDays(3)).statusCode(200)
				.body("size()", is(2))
				.body("find { it.name == 'Fuel station' }.visits", is(3))
				.body("find { it.name == 'Fuel station' }.id", notNullValue());

		// named again near it: renamed, not a second one
		name(STATION - 30, "Aral").statusCode(204);
		int id = places(START.minusHours(1), START.plusDays(3)).statusCode(200)
				.body("name", hasItems("Aral"))
				.body("name", not(hasItem("Fuel station")))
				.extract().path("find { it.name == 'Aral' }.id");

		given().contentType(ContentType.JSON).body(Map.of("name", "Shell"))
				.put("/api/track/{carId}/places/{id}", carId, id).then().statusCode(204);

		// shown without visits, e.g. after the points were deleted
		places(START.plusDays(10), START.plusDays(11)).statusCode(200)
				.body("size()", is(1))
				.body("[0].name", is("Shell"))
				.body("[0].visits", is(0))
				.body("[0].lastVisit", nullValue());

		given().delete("/api/track/{carId}/places/{id}", carId, id).then().statusCode(204);
		places(START.plusDays(10), START.plusDays(11)).statusCode(200).body("size()", is(0));
	}

	@Test
	void shouldCheckTheName()
	{
		name(0, " ").statusCode(400).body("[0]", containsString("name must be set"));
		name(0, "x".repeat(101)).statusCode(400).body("[0]", containsString("not be longer than 100"));
		given().contentType(ContentType.JSON).body(Map.of("name", "Home"))
				.put("/api/track/{carId}/places", carId).then().statusCode(400)
				.body("[0]", containsString("position"));
	}

	@Test
	void shouldCheckTheRange()
	{
		places(START, START.plusDays(32)).statusCode(400);
	}

	@Test
	void shouldNotShowOrNameThePlacesOfAnotherCar()
	{
		given().queryParam("from", START.toString()).queryParam("to", START.plusDays(1).toString())
				.get("/api/track/{carId}/places", otherCarId).then().statusCode(404);
		given().contentType(ContentType.JSON).body(Map.of("lat", 50.0, "lon", 10.0, "name", "Home"))
				.put("/api/track/{carId}/places", otherCarId).then().statusCode(404);
		given().delete("/api/track/{carId}/places/{id}", otherCarId, 1).then().statusCode(404);
	}
}
