package de.codeflowwizardry.carledger.rest.car.trip;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
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
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Peter's car is at 50000 km, two days ago he drove to 50100. Yesterday the
 * instrument cluster was replaced, the new one shows 100 km.
 */
@QuarkusTest
@TestHTTPEndpoint(TripResource.class)
@TestSecurity(user = "peter", roles = {
		"user"
})
class OdometerResetResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	EntityManager em;

	long carId;
	long otherCarId;

	final OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
	final OffsetDateTime resetAt = now.minusDays(1);

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();
		carId = car(account("peter"), "Peters car");
		otherCarId = car(account("paul"), "Pauls car");
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
		car.setManufactureYear(2020);
		car.setOdometer(50_000);
		car.setOdometerDate(LocalDate.now().minusMonths(1));
		car.setTripLog(true);
		carRepository.persist(car);
		return car.getId();
	}

	private ValidatableResponse reset(long car, UUID id, OffsetDateTime at, int odometer, Integer before)
	{
		Map<String, Object> body = new HashMap<>();
		body.put("resetAt", at.toString());
		body.put("odometer", odometer);
		body.put("odometerBefore", before);
		body.put("reason", "Instrument cluster replaced");
		return given()
				.contentType(ContentType.JSON)
				.pathParam("carId", car)
				.body(body)
				.when()
				.put("resets/{id}", id)
				.then();
	}

	private ValidatableResponse deleteReset(long car, UUID id, String reason)
	{
		return given()
				.contentType(ContentType.JSON)
				.pathParam("carId", car)
				.body(Map.of("reason", reason))
				.when()
				.post("resets/{id}/delete", id)
				.then();
	}

	private ValidatableResponse start(UUID id, int odometer, OffsetDateTime at, boolean queued)
	{
		Map<String, Object> body = new HashMap<>();
		body.put("type", TripType.PRIVATE);
		body.put("startOdometer", odometer);
		body.put("startedAt", at.toString());
		body.put("queued", queued);
		return given().contentType(ContentType.JSON).pathParam("carId", carId).body(body)
				.when().put("{id}", id).then();
	}

	private ValidatableResponse finish(UUID id, int odometer, OffsetDateTime at, boolean queued)
	{
		Map<String, Object> body = new HashMap<>();
		body.put("endOdometer", odometer);
		body.put("endedAt", at.toString());
		body.put("queued", queued);
		return given().contentType(ContentType.JSON).pathParam("carId", carId).body(body)
				.when().put("{id}/finish", id).then();
	}

	private ValidatableResponse past(int from, int to, OffsetDateTime start, OffsetDateTime end)
	{
		Map<String, Object> body = new HashMap<>();
		body.put("type", TripType.PRIVATE);
		body.put("startOdometer", from);
		body.put("startedAt", start.toString());
		body.put("endOdometer", to);
		body.put("endedAt", end.toString());
		return given().contentType(ContentType.JSON).pathParam("carId", carId).body(body)
				.when().put("{id}/past", UUID.randomUUID()).then();
	}

	private void trip(int from, int to, OffsetDateTime start, OffsetDateTime end)
	{
		UUID id = UUID.randomUUID();
		start(id, from, start, false).statusCode(201);
		finish(id, to, end, false).statusCode(200);
	}

	private ValidatableResponse current()
	{
		return given().pathParam("carId", carId).when().get("current").then().statusCode(200);
	}

	private ValidatableResponse odometer()
	{
		return given().basePath("/api/car/my").when().get("/" + carId).then().statusCode(200);
	}

	@Test
	void shouldCountFromTheReset()
	{
		trip(50_000, 50_100, now.minusDays(2), now.minusDays(2).plusHours(1));
		UUID id = UUID.randomUUID();
		reset(carId, id, resetAt, 100, 50_100)
				.statusCode(201)
				.body("odometer", is(100))
				.body("odometerBefore", is(50_100))
				.body("createdBy", is("peter"));
		// sent again
		reset(carId, id, resetAt, 100, 50_100).statusCode(200);
		reset(carId, id, resetAt, 120, 50_100).statusCode(409).body("reason", is("ID_IN_USE"));

		current().body("minimumOdometer", is(100)).body("estimatedOdometer", is(100));
		odometer().body("currentOdometer.value", is(100));
		given().pathParam("carId", carId).when().get("resets").then()
				.statusCode(200)
				.body("id", contains(id.toString()));

		start(UUID.randomUUID(), 99, now.minusHours(2), false)
				.statusCode(409)
				.body("reason", is("ODOMETER_BELOW_PREVIOUS"))
				.body("minimumOdometer", is(100));
		trip(100, 150, now.minusHours(2), now.minusHours(1));
		odometer().body("currentOdometer.value", is(150));
	}

	@Test
	void shouldCheckTripsBeforeTheResetAgainstTheOldReadings()
	{
		reset(carId, UUID.randomUUID(), resetAt, 100, 50_100).statusCode(201);

		past(50_000, 50_080, now.minusDays(3), now.minusDays(3).plusHours(1)).statusCode(201);
		// above the last reading of the old instrument cluster
		past(50_080, 50_200, now.minusDays(2), now.minusDays(2).plusHours(1))
				.statusCode(409)
				.body("reason", is("ODOMETER_ABOVE_NEXT"))
				.body("maximumOdometer", is(50_100));
		past(50_080, 50_100, resetAt.minusHours(1), resetAt.plusHours(1))
				.statusCode(409)
				.body("reason", is("ODOMETER_RESET"));
		// ends at the reset
		past(50_080, 50_100, resetAt.minusHours(1), resetAt).statusCode(201);
		current().body("minimumOdometer", is(100));
	}

	@Test
	void shouldStoreAQueuedTripDuringAResetForAReview()
	{
		UUID id = UUID.randomUUID();
		// started offline, before the reset arrived
		start(id, 50_000, resetAt.minusHours(1), true).statusCode(201);
		QuarkusTransaction.requiringNew()
				.run(() -> em.createNativeQuery("insert into odometer_reset (id, rs_car_id, rs_reset_at, rs_odometer,"
						+ " rs_reason, rs_created_by) values (?1, ?2, ?3, 100, 'Replaced', 'peter')")
						.setParameter(1, UUID.randomUUID())
						.setParameter(2, carId)
						.setParameter(3, resetAt)
						.executeUpdate());

		finish(id, 50_050, resetAt.plusHours(1), true)
				.statusCode(200)
				.body("reviewReason", is("ODOMETER_RESET"));
	}

	@Test
	void shouldNotResetDuringATrip()
	{
		UUID open = UUID.randomUUID();
		start(open, 50_000, now.minusHours(2), false).statusCode(201);
		reset(carId, UUID.randomUUID(), now.minusHours(1), 0, null)
				.statusCode(409)
				.body("reason", is("OVERLAPS"));
		// before it, the trip starts above
		reset(carId, UUID.randomUUID(), now.minusHours(3), 50_001, null)
				.statusCode(409)
				.body("reason", is("ODOMETER_ABOVE_NEXT"))
				.body("maximumOdometer", is(50_000));
	}

	@Test
	void shouldValidateTheReset()
	{
		trip(50_000, 50_100, now.minusDays(2), now.minusDays(2).plusHours(1));
		reset(carId, UUID.randomUUID(), resetAt, 100, 50_050)
				.statusCode(409)
				.body("reason", is("ODOMETER_BELOW_PREVIOUS"))
				.body("minimumOdometer", is(50_100));
		reset(carId, UUID.randomUUID(), now.plusHours(1), 100, null)
				.statusCode(400)
				.body("[0]", is("Time must not be in the future!"));
		given().contentType(ContentType.JSON).pathParam("carId", carId).body(Map.of())
				.when().put("resets/{id}", UUID.randomUUID()).then()
				.statusCode(400)
				.body("$", hasItems("Time must be set!", "Odometer must be set!", "A reason must be given!"));

		UUID id = UUID.randomUUID();
		reset(carId, id, resetAt, 100, null).statusCode(201);
		reset(carId, UUID.randomUUID(), resetAt, 200, null)
				.statusCode(409)
				.body("reason", is("OVERLAPS"));
	}

	@Test
	void shouldResetOnlyOwnCarsWithTheTripLogOn()
	{
		reset(otherCarId, UUID.randomUUID(), resetAt, 100, null).statusCode(404);
		given().pathParam("carId", otherCarId).when().get("resets").then().statusCode(404);

		QuarkusTransaction.requiringNew()
				.run(() -> carRepository.findById(carId, "peter", Action.WRITE).setTripLog(false));
		reset(carId, UUID.randomUUID(), resetAt, 100, null).statusCode(400);
	}

	@Test
	void shouldDeleteAResetWithAReasonAndKeepIt()
	{
		trip(50_000, 50_100, now.minusDays(2), now.minusDays(2).plusHours(1));
		UUID id = UUID.randomUUID();
		reset(carId, id, resetAt, 100, null).statusCode(201);

		deleteReset(carId, id, " ").statusCode(400);
		deleteReset(otherCarId, id, "Not mine").statusCode(404);
		deleteReset(carId, id, "Wrong car").statusCode(200).body("deleteReason", is("Wrong car"));
		// sent again
		deleteReset(carId, id, "Other").statusCode(200).body("deleteReason", is("Wrong car"));

		current().body("minimumOdometer", is(50_100));
		odometer().body("currentOdometer.value", is(50_100));
		given().pathParam("carId", carId).when().get("resets").then()
				.body("deletedAt", contains(notNullValue()));
		// a deleted one doesn't block another at its time
		reset(carId, UUID.randomUUID(), resetAt, 100, null).statusCode(201);
	}

	@Test
	void shouldOnlyDeleteAResetInTheDatabase()
	{
		UUID id = UUID.randomUUID();
		reset(carId, id, resetAt, 100, null).statusCode(201);
		assertRejected("update odometer_reset set rs_odometer = 200 where id = ?1", id, "can only be deleted");
		assertRejected("update odometer_reset set rs_deleted_at = now(), rs_deleted_by = 'peter',"
				+ " rs_delete_reason = 'x', rs_odometer = 200 where id = ?1", id, "can only be deleted");

		assertEquals(1, update("update odometer_reset set rs_deleted_at = now(), rs_deleted_by = 'peter',"
				+ " rs_delete_reason = 'Wrong' where id = ?1", id));
		assertRejected("update odometer_reset set rs_delete_reason = 'Other' where id = ?1", id, "a deleted reset");
	}

	private int update(String sql, UUID id)
	{
		return QuarkusTransaction.requiringNew()
				.call(() -> em.createNativeQuery(sql).setParameter(1, id).executeUpdate());
	}

	private void assertRejected(String sql, UUID id, String message)
	{
		RuntimeException exception = assertThrows(RuntimeException.class, () -> update(sql, id));
		Throwable cause = exception;
		while (cause.getCause() != null)
		{
			cause = cause.getCause();
		}
		assertTrue(cause.getMessage().contains(message), cause.getMessage());
	}
}
