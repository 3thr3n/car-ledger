package de.codeflowwizardry.carledger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.tracker.DeviceInput;
import de.codeflowwizardry.carledger.rest.device.TrackPointInput;
import de.codeflowwizardry.carledger.tracker.DeviceService;
import de.codeflowwizardry.carledger.tracker.DeviceService.AuthenticatedDevice;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * The metrics for Prometheus: the ones of Quarkus and the own ones. In the
 * tests on the port of the app, in prod on the management port.
 */
@QuarkusTest
class MetricsTest
{
	@Inject
	AccountRepository accountRepository;
	@Inject
	CarRepository carRepository;
	@Inject
	DeviceService deviceService;

	long carId;
	AuthenticatedDevice device;

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();
		AccountEntity account = new AccountEntity();
		account.setUserId("peter");
		account.setMaxCars(1);
		accountRepository.persist(account);
		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName("Car");
		carRepository.persist(car);
		carId = car.getId();
		device = new AuthenticatedDevice(deviceService.create(carId, new DeviceInput("ESP32"), "peter").device().id(),
				carId);
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	private static String metrics()
	{
		return given().when().get("/q/metrics").then().statusCode(200).extract().asString();
	}

	/** the value of a line of the metrics, 0 without it */
	private static double value(String metrics, String line)
	{
		Matcher matcher = Pattern.compile("^" + Pattern.quote(line) + " (\\S+)$", Pattern.MULTILINE)
				.matcher(metrics);
		return matcher.find() ? Double.parseDouble(matcher.group(1)) : 0;
	}

	@Test
	void shouldCountThePointsOfTheTrackers()
	{
		String accepted = "carledger_tracker_points_total{result=\"accepted\"}";
		String duplicate = "carledger_tracker_points_total{result=\"duplicate\"}";
		String rejected = "carledger_tracker_points_total{result=\"rejected\"}";
		String before = metrics();
		OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).minusMinutes(1);

		deviceService.ingest(device, List.of(new TrackPointInput(ts, 50.0, 10.0, 30.0, null, null),
				new TrackPointInput(ts.plusSeconds(1), 50.0, 10.0, 30.0, null, null),
				// a position outside of the earth
				new TrackPointInput(ts.plusSeconds(2), 95.0, 10.0, 30.0, null, null)));
		deviceService.ingest(device, List.of(new TrackPointInput(ts, 50.0, 10.0, 30.0, null, null)));

		String after = metrics();
		assertEquals(2, value(after, accepted) - value(before, accepted));
		assertEquals(1, value(after, duplicate) - value(before, duplicate));
		assertEquals(1, value(after, rejected) - value(before, rejected));
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldTimeTheTrackOfTheMap()
	{
		OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC);
		given().queryParam("from", to.minusDays(1).toString()).queryParam("to", to.toString())
				.when().get("/api/track/{carId}", carId).then().statusCode(200);

		given().when().get("/q/metrics").then()
				.body(containsString("carledger_track_seconds_count{kind=\"track\"}"))
				.body(containsString("carledger_track_seconds_bucket{kind=\"track\",le=\"1.0\"}"));
	}

	@Test
	void shouldHaveTheMetricsOfQuarkus()
	{
		// the requests count once there was one
		given().when().get("/api/version").then().statusCode(200);
		given().when().get("/q/metrics").then()
				.body(containsString("jvm_memory_used_bytes"))
				.body(containsString("http_server_requests_seconds_count"))
				.body(containsString("agroal_active_count"))
				.body(containsString("hibernate_sessions_open_total"));
	}
}
