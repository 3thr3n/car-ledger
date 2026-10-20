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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.TripService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.TripType;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
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
 * Peter's car is at 50000 km, the trips are checked against it and against each
 * other in the order they were driven
 */
@QuarkusTest
@TestHTTPEndpoint(TripResource.class)
@TestSecurity(user = "peter", roles = {
		"user"
})
class TripResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	TripService tripService;

	@Inject
	EntityManager em;

	long carId;
	long otherCarId;

	final OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);

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

	private static Map<String, Object> startBody(TripType type, int odometer, OffsetDateTime at, boolean queued)
	{
		Map<String, Object> body = new HashMap<>();
		body.put("type", type);
		body.put("startOdometer", odometer);
		body.put("startedAt", at.toString());
		body.put("queued", queued);
		return body;
	}

	private static Map<String, Object> finishBody(int odometer, OffsetDateTime at, boolean queued)
	{
		Map<String, Object> body = new HashMap<>();
		body.put("endOdometer", odometer);
		body.put("endedAt", at.toString());
		body.put("queued", queued);
		return body;
	}

	private ValidatableResponse start(long car, UUID id, Map<String, Object> body)
	{
		return given()
				.contentType(ContentType.JSON)
				.pathParam("carId", car)
				.body(body)
				.when()
				.put("{id}", id)
				.then();
	}

	private ValidatableResponse finish(long car, UUID id, Map<String, Object> body)
	{
		return given()
				.contentType(ContentType.JSON)
				.pathParam("carId", car)
				.body(body)
				.when()
				.put("{id}/finish", id)
				.then();
	}

	private ValidatableResponse current()
	{
		return given().pathParam("carId", carId).when().get("current").then().statusCode(200);
	}

	/** a finished private trip */
	private UUID trip(int from, int to, OffsetDateTime start, OffsetDateTime end)
	{
		UUID id = UUID.randomUUID();
		start(carId, id, startBody(TripType.PRIVATE, from, start, false)).statusCode(201);
		finish(carId, id, finishBody(to, end, false)).statusCode(200);
		return id;
	}

	@Test
	void shouldStartAndFinishATrip()
	{
		UUID id = UUID.randomUUID();
		current().body("open", nullValue()).body("minimumOdometer", is(50_000));

		start(carId, id, startBody(TripType.PRIVATE, 50_000, now.minusHours(1), false))
				.statusCode(201)
				.header("Location", endsWith("trip/" + carId + "/" + id))
				.body("id", is(id.toString()))
				.body("distance", nullValue())
				.body("restored", is(false));
		current().body("open.id", is(id.toString()));

		finish(carId, id, finishBody(50_120, now, false))
				.statusCode(200)
				.body("distance", is(120))
				.body("reviewReason", nullValue())
				.body("finishedAt", notNullValue());
		current().body("open", nullValue()).body("minimumOdometer", is(50_120));

		given().pathParam("carId", carId).when().get().then()
				.statusCode(200)
				.body("total", is(1))
				.body("data[0].endOdometer", is(50_120));
	}

	@Test
	void shouldStartNoTripWithTheTripLogOff()
	{
		UUID open = UUID.randomUUID();
		start(carId, open, startBody(TripType.PRIVATE, 50_000, now.minusHours(1), false)).statusCode(201);
		tripLog(false);

		// the open trip can still be finished
		finish(carId, open, finishBody(50_100, now.minusMinutes(30), false))
				.statusCode(200);
		// also one sent later from the offline queue
		start(carId, UUID.randomUUID(), startBody(TripType.PRIVATE, 50_100, now, true))
				.statusCode(400)
				.body("[0]", is("The trip log of the car is off, turn it on in the settings of the car!"));
	}

	@Test
	void shouldAnswerARepeatedRequestLikeTheFirst()
	{
		UUID id = UUID.randomUUID();
		Map<String, Object> startBody = startBody(TripType.PRIVATE, 50_000, now.minusHours(1), false);
		start(carId, id, startBody).statusCode(201);
		start(carId, id, startBody).statusCode(200).body("id", is(id.toString()));
		start(carId, id, startBody(TripType.PRIVATE, 50_001, now.minusHours(1), false))
				.statusCode(409)
				.body("reason", is("ID_IN_USE"));

		Map<String, Object> finishBody = finishBody(50_050, now, false);
		finish(carId, id, finishBody).statusCode(200);
		finish(carId, id, finishBody).statusCode(200).body("distance", is(50));
		finish(carId, id, finishBody(50_060, now, false))
				.statusCode(409)
				.body("reason", is("ALREADY_FINISHED"));

		given().pathParam("carId", carId).when().get().then().body("total", is(1));
	}

	@Test
	void shouldRejectAStartBelowTheHighestReading()
	{
		start(carId, UUID.randomUUID(), startBody(TripType.PRIVATE, 49_999, now, false))
				.statusCode(409)
				.body("reason", is("ODOMETER_BELOW_PREVIOUS"))
				.body("minimumOdometer", is(50_000));
		start(carId, UUID.randomUUID(), startBody(TripType.PRIVATE, 50_000, now, false)).statusCode(201);
	}

	@Test
	void shouldAllowOnlyOneOpenTrip()
	{
		start(carId, UUID.randomUUID(), startBody(TripType.PRIVATE, 50_000, now.minusHours(1), false))
				.statusCode(201);
		start(carId, UUID.randomUUID(), startBody(TripType.PRIVATE, 50_100, now, true))
				.statusCode(409)
				.body("reason", is("OPEN_TRIP"));
	}

	@Test
	void shouldValidateTheInput()
	{
		start(carId, UUID.randomUUID(), Map.of())
				.statusCode(400)
				.body("$", hasItems("Type must be set!", "Start odometer must be set!", "Start time must be set!"));
		start(carId, UUID.randomUUID(), startBody(TripType.PRIVATE, 50_000, now.plusHours(1), false))
				.statusCode(400)
				.body("[0]", is("Start time must not be in the future!"));

		UUID id = UUID.randomUUID();
		start(carId, id, startBody(TripType.PRIVATE, 50_000, now.minusHours(1), false)).statusCode(201);
		finish(carId, id, finishBody(49_999, now, true))
				.statusCode(400)
				.body("[0]", is("End odometer must not be below the start of 50000 km!"));
		finish(carId, id, finishBody(50_010, now.minusHours(2), false))
				.statusCode(400)
				.body("[0]", is("End time must not be before the start!"));
		finish(carId, UUID.randomUUID(), finishBody(50_010, now, false)).statusCode(404);
	}

	@Test
	void shouldNeedTheDetailsForABusinessTrip()
	{
		UUID id = UUID.randomUUID();
		Map<String, Object> startBody = startBody(TripType.BUSINESS, 50_000, now.minusHours(1), false);
		startBody.put("destination", " Munich ");
		start(carId, id, startBody).statusCode(201).body("destination", is("Munich"));

		finish(carId, id, finishBody(50_200, now, false))
				.statusCode(400)
				.body("[0]", is("A business trip needs a destination, a purpose and a business partner!"));

		Map<String, Object> finishBody = finishBody(50_200, now, false);
		finishBody.put("destination", "Berlin");
		finishBody.put("purpose", "Customer visit");
		finishBody.put("businessPartner", "ACME");
		finish(carId, id, finishBody).statusCode(409).body("reason", is("DETAIL_CHANGED"));

		finishBody.put("destination", "Munich");
		finish(carId, id, finishBody)
				.statusCode(200)
				.body("purpose", is("Customer visit"))
				.body("businessPartner", is("ACME"));
	}

	@Test
	void shouldRejectALiveTripThatDoesNotFitALaterOne()
	{
		trip(50_100, 50_200, now.minusHours(2), now.minusHours(1));

		UUID id = UUID.randomUUID();
		start(carId, id, startBody(TripType.PRIVATE, 50_000, now.minusHours(5), false)).statusCode(201);
		finish(carId, id, finishBody(50_150, now.minusHours(4), false))
				.statusCode(409)
				.body("reason", is("ODOMETER_ABOVE_NEXT"))
				.body("maximumOdometer", is(50_100));
	}

	@Test
	void shouldStoreAQueuedTripThatDoesNotFitForAReview()
	{
		trip(50_100, 50_200, now.minusHours(2), now.minusHours(1));

		UUID above = UUID.randomUUID();
		start(carId, above, startBody(TripType.PRIVATE, 50_000, now.minusHours(5), true))
				.statusCode(201)
				.body("reviewReason", nullValue());
		finish(carId, above, finishBody(50_150, now.minusHours(4), true))
				.statusCode(200)
				.body("reviewReason", is("ODOMETER_ABOVE_NEXT"));

		// on the road while the other trip was
		UUID overlapping = UUID.randomUUID();
		start(carId, overlapping, startBody(TripType.PRIVATE, 50_200, now.minusMinutes(90), true))
				.statusCode(201)
				.body("reviewReason", is("OVERLAPS"));
		finish(carId, overlapping, finishBody(50_210, now.minusMinutes(80), true))
				.statusCode(200)
				.body("reviewReason", is("OVERLAPS"));

		UUID below = UUID.randomUUID();
		start(carId, below, startBody(TripType.PRIVATE, 50_150, now.minusMinutes(30), true))
				.statusCode(201)
				.body("reviewReason", is("ODOMETER_BELOW_PREVIOUS"));
	}

	@Test
	void shouldCheckTripsInTheOrderTheyWereDriven()
	{
		trip(50_100, 50_200, now.minusHours(2), now.minusHours(1));

		// arrives later, but was driven before
		UUID earlier = UUID.randomUUID();
		start(carId, earlier, startBody(TripType.PRIVATE, 50_020, now.minusHours(5), true)).statusCode(201);
		finish(carId, earlier, finishBody(50_090, now.minusHours(4), true))
				.statusCode(200)
				.body("reviewReason", nullValue());

		given().pathParam("carId", carId).when().get().then()
				.body("data.startOdometer", contains(50_100, 50_020));
	}

	@Test
	void shouldNotShowOrChangeTripsOfOtherUsers()
	{
		start(otherCarId, UUID.randomUUID(), startBody(TripType.PRIVATE, 50_000, now, false)).statusCode(404);
		given().pathParam("carId", otherCarId).when().get("current").then().statusCode(404);

		UUID paulsTrip = UUID.randomUUID();
		tripService.start(otherCarId, paulsTrip, new TripStartInput(TripType.PRIVATE, 50_000,
				now.minusHours(1), null, null, null, false), "paul");

		given().pathParam("carId", otherCarId).when().get("{id}", paulsTrip).then().statusCode(404);
		given().pathParam("carId", otherCarId).when().get().then().body("total", is(0));
		finish(otherCarId, paulsTrip, finishBody(50_100, now, false)).statusCode(404);
		// the id is taken, but nothing of the trip is shown
		start(carId, paulsTrip, startBody(TripType.PRIVATE, 50_000, now, false))
				.statusCode(409)
				.body("reason", is("ID_IN_USE"))
				.body("minimumOdometer", nullValue());
	}

	@Test
	void shouldNotChangeATripInTheDatabase()
	{
		UUID finished = trip(50_000, 50_100, now.minusHours(3), now.minusHours(2));
		assertRejected("update trip set tp_end_odometer = 50200 where id = ?1", finished, "a finished trip");
		assertRejected("update trip set tp_purpose = 'Other' where id = ?1", finished, "a finished trip");

		UUID open = UUID.randomUUID();
		start(carId, open, startBody(TripType.PRIVATE, 50_100, now.minusHours(1), false)).statusCode(201);
		assertRejected("update trip set tp_start_odometer = 50150 where id = ?1", open, "the start");
		assertRejected("update trip set tp_started_at = now() where id = ?1", open, "the start");

		// a missing detail can be added, but not changed
		assertEquals(1, update("update trip set tp_destination = 'Munich' where id = ?1", open));
		assertRejected("update trip set tp_destination = 'Berlin' where id = ?1", open, "only be added");
	}

	private void tripLog(boolean on)
	{
		QuarkusTransaction.requiringNew()
				.run(() -> carRepository.findById(carId, "peter", Action.WRITE).setTripLog(on));
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

	private ValidatableResponse delete(long car, UUID id, String reason)
	{
		return given()
				.contentType(ContentType.JSON)
				.pathParam("carId", car)
				.body(Map.of("reason", reason))
				.when()
				.post("{id}/delete", id)
				.then();
	}

	@Test
	void shouldDeleteATripWithAReasonAndKeepIt()
	{
		UUID id = trip(50_000, 50_100, now.minusHours(3), now.minusHours(2));
		current().body("minimumOdometer", is(50_100)).body("estimatedOdometer", is(50_100));

		delete(carId, id, "  Entered twice  ")
				.statusCode(200)
				.body("deletedAt", notNullValue())
				.body("deletedBy", is("peter"))
				.body("deleteReason", is("Entered twice"));
		// sent again, it keeps the first deletion
		delete(carId, id, "Other reason").statusCode(200).body("deleteReason", is("Entered twice"));

		// still listed, no longer counted
		given().pathParam("carId", carId).when().get().then()
				.body("total", is(1))
				.body("data[0].deleteReason", is("Entered twice"))
				.body("data[0].distance", is(100));
		current().body("minimumOdometer", is(50_000)).body("estimatedOdometer", is(50_000));
		start(carId, UUID.randomUUID(), startBody(TripType.PRIVATE, 50_000, now.minusHours(1), false))
				.statusCode(201)
				.body("reviewReason", nullValue());
	}

	@Test
	void shouldDeleteAnOpenTripAndStartTheNext()
	{
		UUID open = UUID.randomUUID();
		start(carId, open, startBody(TripType.PRIVATE, 50_000, now.minusHours(2), false)).statusCode(201);

		delete(carId, open, "Started by mistake").statusCode(200);
		current().body("open", nullValue());

		start(carId, UUID.randomUUID(), startBody(TripType.PRIVATE, 50_000, now.minusHours(1), false))
				.statusCode(201);
		// e.g. a finish sent later from the offline queue
		finish(carId, open, finishBody(50_050, now, true))
				.statusCode(409)
				.body("reason", is("DELETED"));
	}

	@Test
	void shouldDeleteOnlyWithAReasonAndOnlyTripsOfOwnCars()
	{
		UUID id = trip(50_000, 50_100, now.minusHours(3), now.minusHours(2));
		delete(carId, id, " ").statusCode(400).body("[0]", is("A reason must be given!"));
		delete(carId, id, "x".repeat(501)).statusCode(400);
		delete(otherCarId, id, "Not mine").statusCode(404);
		delete(carId, UUID.randomUUID(), "Unknown").statusCode(404);
		given().pathParam("carId", carId).when().get().then().body("data[0].deletedAt", nullValue());
	}

	@Test
	void shouldOnlySetTheDeletionOnceInTheDatabase()
	{
		UUID finished = trip(50_000, 50_100, now.minusHours(4), now.minusHours(3));
		UUID open = UUID.randomUUID();
		start(carId, open, startBody(TripType.PRIVATE, 50_100, now.minusHours(1), false)).statusCode(201);
		assertRejected("update trip set tp_deleted_at = now(), tp_deleted_by = 'peter', tp_delete_reason = 'x',"
				+ " tp_end_odometer = 50200, tp_ended_at = now() where id = ?1", open, "changes nothing else");

		assertEquals(1, update("update trip set tp_deleted_at = now(), tp_deleted_by = 'peter',"
				+ " tp_delete_reason = 'Wrong' where id = ?1", finished));
		assertRejected("update trip set tp_delete_reason = 'Other' where id = ?1", finished, "a deleted trip");
	}

	private ValidatableResponse addPast(UUID id, int from, int to, OffsetDateTime start, OffsetDateTime end)
	{
		Map<String, Object> body = new HashMap<>();
		body.put("type", TripType.PRIVATE);
		body.put("startOdometer", from);
		body.put("startedAt", start.toString());
		body.put("endOdometer", to);
		body.put("endedAt", end.toString());
		return given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(body)
				.when()
				.put("{id}/past", id)
				.then();
	}

	@Test
	void shouldAddAForgottenTripWhileAnotherIsOpen()
	{
		UUID open = UUID.randomUUID();
		start(carId, open, startBody(TripType.PRIVATE, 50_100, now.minusHours(1), false))
				.statusCode(201)
				.body("enteredLater", is(false));

		UUID past = UUID.randomUUID();
		addPast(past, 50_000, 50_080, now.minusHours(5), now.minusHours(4))
				.statusCode(201)
				.body("distance", is(80))
				.body("finishedAt", notNullValue())
				.body("enteredLater", is(true));
		// sent again
		addPast(past, 50_000, 50_080, now.minusHours(5), now.minusHours(4)).statusCode(200);
		addPast(past, 50_000, 50_090, now.minusHours(5), now.minusHours(4))
				.statusCode(409)
				.body("reason", is("ID_IN_USE"));

		current().body("open.id", is(open.toString()));
		given().pathParam("carId", carId).when().get().then().body("total", is(2));
	}

	@Test
	void shouldRejectAForgottenTripThatDoesNotFit()
	{
		start(carId, UUID.randomUUID(), startBody(TripType.PRIVATE, 50_100, now.minusHours(1), false))
				.statusCode(201);

		// on the road at the same time as the open trip
		addPast(UUID.randomUUID(), 50_000, 50_050, now.minusHours(2), now.minusMinutes(30))
				.statusCode(409)
				.body("reason", is("OVERLAPS"));
		// ends above the start of the later trip
		addPast(UUID.randomUUID(), 50_000, 50_150, now.minusHours(5), now.minusHours(4))
				.statusCode(409)
				.body("reason", is("ODOMETER_ABOVE_NEXT"))
				.body("maximumOdometer", is(50_100));
		// starts below the odometer of the car
		addPast(UUID.randomUUID(), 49_000, 49_050, now.minusHours(5), now.minusHours(4))
				.statusCode(409)
				.body("reason", is("ODOMETER_BELOW_PREVIOUS"))
				.body("minimumOdometer", is(50_000));
		addPast(UUID.randomUUID(), 50_000, 49_990, now.minusHours(5), now.minusHours(4))
				.statusCode(400)
				.body("[0]", is("End odometer must not be below the start!"));
		addPast(UUID.randomUUID(), 50_000, 50_010, now.minusHours(5), now.plusHours(1))
				.statusCode(400)
				.body("[0]", is("End time must not be in the future!"));
	}

	@Test
	void shouldAddNoForgottenTripWithTheTripLogOff()
	{
		tripLog(false);
		addPast(UUID.randomUUID(), 50_000, 50_050, now.minusHours(5), now.minusHours(4))
				.statusCode(400)
				.body("[0]", is("The trip log of the car is off, turn it on in the settings of the car!"));
	}

	@Test
	void shouldStartOnlyOneOfTwoTripsAtTheSameTime()
	{
		List<CompletableFuture<Integer>> starts = List.of(UUID.randomUUID(), UUID.randomUUID()).stream()
				.map(id -> CompletableFuture.supplyAsync(() -> start(carId, id,
						startBody(TripType.PRIVATE, 50_000, now, false)).extract().statusCode()))
				.toList();

		assertEquals(List.of(201, 409), starts.stream().map(CompletableFuture::join).sorted().toList());
	}

	@Test
	void shouldBeAReadingOfTheOdometer()
	{
		trip(50_000, 50_500, now.minusHours(2), now.minusHours(1));

		given().basePath("/api/car/my").when().get("/" + carId).then()
				.statusCode(200)
				.body("currentOdometer.value", is(50_500))
				.body("currentOdometer.reading", is(50_500));
		current().body("estimatedOdometer", is(50_500));
	}

	@Test
	void shouldListByYear()
	{
		trip(50_000, 50_100, now.minusYears(1), now.minusYears(1).plusHours(1));
		trip(50_100, 50_200, now.minusHours(2), now.minusHours(1));
		trip(50_200, 50_300, now.minusMinutes(50), now.minusMinutes(40));

		given().pathParam("carId", carId).when().get("years").then()
				.body("$", contains(now.getYear(), now.getYear() - 1));
		given().pathParam("carId", carId).queryParam("year", now.getYear()).queryParam("size", 1).when().get()
				.then()
				.body("total", is(2))
				.body("data.startOdometer", contains(50_200));
	}
}
