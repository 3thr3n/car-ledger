package de.codeflowwizardry.carledger.rest.car.trip;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.TripService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.TripType;
import de.codeflowwizardry.carledger.data.factory.MaintenanceBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
import de.codeflowwizardry.carledger.rest.car.maintenance.MaintenanceBillInput;
import de.codeflowwizardry.carledger.rest.car.tracker.DeviceInput;
import de.codeflowwizardry.carledger.rest.device.TrackPointInput;
import de.codeflowwizardry.carledger.tracker.DeviceService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Peter's car with an odometer of 50,000 km and a tracker. Two days ago it made
 * two drives of 10 and 5 minutes north at 50 km/h, with a stop of 20 minutes
 * between them while the tracker kept sending.
 */
@QuarkusTest
@TestHTTPEndpoint(TripResource.class)
@TestSecurity(user = "peter", roles = "user")
class TripSuggestionTest
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
	TripService tripService;
	@Inject
	MaintenanceBillFactory maintenanceBillFactory;

	long carId;
	long otherCarId;
	long deviceId;
	/** the first drive starts at second 60 */
	OffsetDateTime start;

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();
		start = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2).truncatedTo(ChronoUnit.HOURS);
		carId = car(account("peter"));
		otherCarId = car(account("paul"));
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

	private long car(AccountEntity account)
	{
		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName("Car");
		car.setOdometer(50_000);
		car.setOdometerDate(start.toLocalDate().minusYears(1));
		car.setTripLog(true);
		carRepository.persist(car);
		return car.getId();
	}

	/** the drives of the class, from <code>from</code> on */
	private void drives(OffsetDateTime from)
	{
		List<TrackPointInput> points = new ArrayList<>();
		double north = 0;
		int[][] phases = {
				// seconds, meters per second
				{
						60, 0
				}, {
						600, 14
				}, {
						1200, 0
				}, {
						300, 14
				}, {
						600, 0
				}
		};
		int second = 0;
		for (int[] phase : phases)
		{
			for (int i = 0; i < phase[0]; i++)
			{
				north += phase[1];
				points.add(new TrackPointInput(from.plusSeconds(second++), 50 + north * LAT_PER_METER, 10.0,
						(double) phase[1] * 3.6, null, null));
			}
		}
		QuarkusTransaction.requiringNew().run(() -> trackPointRepository.insert(deviceId, carId, points));
	}

	/** the drift radius costs a few meters at the start and the end */
	private static Matcher<Float> about(float km)
	{
		return both(greaterThan(km - 0.1F)).and(lessThan(km + 0.1F));
	}

	private ValidatableResponse suggestions()
	{
		return given().pathParam("carId", carId).when().get("suggestions").then().statusCode(200);
	}

	private void addPast(OffsetDateTime startedAt, int startOdometer, OffsetDateTime endedAt, int endOdometer)
	{
		tripService.addPast(carId, UUID.randomUUID(), new TripPastInput(TripType.PRIVATE, startOdometer, startedAt,
				endOdometer, endedAt, null, null, null), "peter");
	}

	@Test
	void shouldSuggestTheDrivesWithFollowingOdometers()
	{
		drives(start);

		// 8.4 km and 4.2 km, the second starts where the first ends
		suggestions()
				.body("", hasSize(2))
				.body("startOdometer", contains(50_000, 50_008))
				.body("endOdometer", contains(50_008, 50_012))
				.body("[0].distance", about(8.4F))
				.body("[1].distance", about(4.2F));
	}

	@Test
	void shouldStartAtTheEndOfTheLastTrip()
	{
		drives(start);
		// a trip between the drives, it ends after the first
		addPast(start.plusMinutes(15), 50_100, start.plusMinutes(20), 50_105);

		suggestions()
				.body("", hasSize(1))
				.body("[0].startOdometer", is(50_105))
				.body("[0].endOdometer", is(50_109));
	}

	@Test
	void shouldLookBackAtMostSevenDays()
	{
		drives(start.minusDays(6));

		suggestions().body("", empty());
	}

	@Test
	void shouldNotSuggestADriveOfATrip()
	{
		drives(start);
		addPast(start.plusMinutes(2), 50_000, start.plusMinutes(5), 50_002);
		// the window starts after this trip, only the second drive is left
		suggestions().body("", hasSize(1)).body("[0].startOdometer", is(50_002));
	}

	@Test
	void shouldNotSuggestADriveDuringTheOpenTrip()
	{
		drives(start);
		tripService.start(carId, UUID.randomUUID(), new TripStartInput(TripType.PRIVATE, 50_010,
				start.plusMinutes(25), null, null, null, false), "peter");

		suggestions().body("", hasSize(1)).body("[0].distance", about(8.4F));
	}

	@Test
	void shouldStartAtALaterReading()
	{
		drives(start);
		maintenanceBillFactory.create(new MaintenanceBillInput(BigDecimal.valueOf(119), 19,
				start.toLocalDate(), 50_500, BigDecimal.valueOf(80), BigDecimal.valueOf(39), "Oil change", null),
				carId, "peter");

		suggestions().body("startOdometer", contains(50_500, 50_508));
	}

	@Test
	void shouldStartAtTheOdometerOfACarEnteredAfterTheDrive()
	{
		drives(start);
		QuarkusTransaction.requiringNew().run(() -> carRepository.findById(carId, "peter", Action.MANAGE)
				.setOdometerDate(start.toLocalDate().plusDays(1)));

		suggestions().body("startOdometer", contains(50_000, 50_008));
	}

	@Test
	void shouldNotBringBackADismissedDrive()
	{
		drives(start);
		List<String> startedAt = suggestions().extract().path("startedAt");

		// found a little differently later: still dismissed
		given().contentType(ContentType.JSON).pathParam("carId", carId)
				.body(Map.of("startedAt", OffsetDateTime.parse(startedAt.get(0)).plusMinutes(1).toString(),
						"endedAt", OffsetDateTime.parse(startedAt.get(0)).plusMinutes(5).toString()))
				.when().post("suggestions/dismiss").then().statusCode(204);

		suggestions().body("", hasSize(1)).body("[0].distance", about(4.2F));
	}

	@Test
	void shouldValidateADismissal()
	{
		given().contentType(ContentType.JSON).pathParam("carId", carId)
				.body(Map.of("startedAt", start.toString(), "endedAt", start.toString()))
				.when().post("suggestions/dismiss").then().statusCode(400)
				.body("", contains("End time must be after the start time!"));
		given().contentType(ContentType.JSON).pathParam("carId", carId).body(Map.of("startedAt", start.toString()))
				.when().post("suggestions/dismiss").then().statusCode(400);
	}

	@Test
	void shouldBeEmptyWithTheTripLogOff()
	{
		drives(start);
		QuarkusTransaction.requiringNew()
				.run(() -> carRepository.findById(carId, "peter", Action.MANAGE).setTripLog(false));

		suggestions().body("", empty());
	}

	/** start and end of the drives of the map, newest first */
	private List<Map<String, String>> drivesOfTheMap(OffsetDateTime from)
	{
		return given().basePath("/api/track/{carId}").pathParam("carId", carId)
				.queryParam("from", from.toString()).queryParam("to", from.plusHours(1).toString())
				.when().get("drives").then().statusCode(200).extract().path("");
	}

	private ValidatableResponse forDrive(Map<String, String> drive)
	{
		return given().pathParam("carId", carId)
				.queryParam("start", drive.get("start")).queryParam("end", drive.get("end"))
				.when().get("suggestions/drive").then();
	}

	@Test
	void shouldSuggestAPickedDriveFromTheOdometerBeforeIt()
	{
		drives(start);
		addPast(start.minusHours(2), 50_100, start.minusHours(1), 50_110);

		// the second drive alone: not after the first, its odometers aren't known
		forDrive(drivesOfTheMap(start).get(0)).statusCode(200)
				.body("startOdometer", is(50_110))
				.body("endOdometer", is(50_114))
				.body("distance", about(4.2F));
	}

	@Test
	void shouldSuggestADriveOlderThanTheWindowAndADismissedOne()
	{
		OffsetDateTime old = start.minusDays(10);
		drives(old);
		Map<String, String> first = drivesOfTheMap(old).get(1);
		given().contentType(ContentType.JSON).pathParam("carId", carId)
				.body(Map.of("startedAt", first.get("start"), "endedAt", first.get("end")))
				.when().post("suggestions/dismiss").then().statusCode(204);

		forDrive(first).statusCode(200)
				.body("startedAt", is(first.get("start")))
				.body("startOdometer", is(50_000))
				.body("endOdometer", is(50_008))
				.body("distance", about(8.4F));
	}

	@Test
	void shouldValidateThePickedDrive()
	{
		forDrive(Map.of("start", start.toString(), "end", start.toString())).statusCode(400)
				.body("", contains("End time must be after the start time!"));
		forDrive(Map.of("start", start.toString())).statusCode(400);
		forDrive(Map.of("start", start.toString(), "end", start.plusDays(32).toString())).statusCode(400)
				.body("", contains("The drive must not be longer than 31 days!"));
	}

	@Test
	void shouldNotSuggestTheDrivesOfAnotherCar()
	{
		given().pathParam("carId", otherCarId).queryParam("start", start.toString())
				.queryParam("end", start.plusMinutes(5).toString())
				.when().get("suggestions/drive").then().statusCode(404);
		given().pathParam("carId", otherCarId).when().get("suggestions").then().statusCode(404);
		given().contentType(ContentType.JSON).pathParam("carId", otherCarId)
				.body(Map.of("startedAt", start.toString(), "endedAt", start.plusMinutes(5).toString()))
				.when().post("suggestions/dismiss").then().statusCode(404);
	}
}
