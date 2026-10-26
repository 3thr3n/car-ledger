package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.TripService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarAccessEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.CarRole;
import de.codeflowwizardry.carledger.data.TripType;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccessRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.fuel.FuelBillInput;
import de.codeflowwizardry.carledger.rest.car.trip.Trip;
import de.codeflowwizardry.carledger.rest.car.trip.TripFinishInput;
import de.codeflowwizardry.carledger.rest.car.trip.TripStartInput;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Paul drives the car of peter: he sees, adds and changes only his own trips
 * and refuels, the entries show who made them
 */
@QuarkusTest
@TestSecurity(user = "paul", roles = "user")
class DriverTest
{
	private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-01T08:00:00+02:00");
	/** 40 l for 70 €, 600 km */
	private static final Map<String, Object> REFUEL = Map.of("date", "2026-10-02", "total", 70, "vatRate", 19,
			"distance", 600, "unit", 40, "pricePerUnit", 1.75);

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	CarAccessRepository carAccessRepository;

	@Inject
	FuelBillFactory fuelBillFactory;

	@Inject
	TripService tripService;

	@Inject
	EntityManager entityManager;

	long carId;
	/** of peter */
	long refuelId;
	/** of peter, open */
	final UUID tripId = UUID.randomUUID();

	@BeforeEach
	void setup()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			AccountEntity peter = account("peter");
			account("paul");

			CarEntity car = new CarEntity();
			car.setUser(peter);
			car.setName("Golf");
			car.setManufactureYear(2020);
			car.setOdometer(10_000);
			car.setTripLog(true);
			carRepository.persist(car);
			carId = car.getId();
		});
		refuelId = QuarkusTransaction.requiringNew().call(() -> fuelBillFactory.create(
				new FuelBillInput(LocalDate.of(2026, 9, 20), BigDecimal.valueOf(60), 19, BigDecimal.valueOf(500),
						BigDecimal.valueOf(35), BigDecimal.valueOf(1.71), null),
				carId, "peter").getBill().getId());
	}

	@AfterEach
	void cleanup()
	{
		QuarkusTransaction.requiringNew().run(() -> accountRepository.deleteAll());
	}

	private AccountEntity account(String name)
	{
		AccountEntity account = new AccountEntity();
		account.setUserId(name);
		account.setMaxCars(1);
		accountRepository.persist(account);
		return account;
	}

	private void share(CarRole role)
	{
		QuarkusTransaction.requiringNew().run(() -> carAccessRepository.persist(new CarAccessEntity(
				entityManager.find(CarEntity.class, carId), accountRepository.findByIdentifier("paul"), role, null)));
	}

	/** an open trip of peter */
	private void peterDrives()
	{
		tripService.start(carId, tripId, new TripStartInput(TripType.PRIVATE, 10_000, START, null, null, null, false),
				"peter");
	}

	private ValidatableResponse refuel()
	{
		return given().contentType(ContentType.JSON).body(REFUEL)
				.put("/api/bill/{carId}/fuel", carId).then();
	}

	private ValidatableResponse start(UUID id, int odometer, OffsetDateTime at)
	{
		return given().contentType(ContentType.JSON)
				.body(Map.of("type", "PRIVATE", "startOdometer", odometer, "startedAt", at.toString()))
				.put("/api/trip/{carId}/{id}", carId, id).then();
	}

	private ValidatableResponse finish(UUID id, int odometer, OffsetDateTime at)
	{
		return given().contentType(ContentType.JSON)
				.body(Map.of("endOdometer", odometer, "endedAt", at.toString()))
				.put("/api/trip/{carId}/{id}/finish", carId, id).then();
	}

	@Test
	void shouldAddAndSeeOnlyTheOwnRefuels()
	{
		share(CarRole.DRIVER);

		refuel().statusCode(anyOf(is(200), is(201), is(202)));

		given().get("/api/bill/{carId}/fuel/all", carId).then().statusCode(200)
				.body("total", is(1))
				.body("data[0].createdBy", is("paul"))
				.body("data[0].distance", is(600.0F));
		// the refuel of peter isn't for him
		given().get("/api/bill/{carId}/fuel/{billId}", carId, refuelId).then().statusCode(404);
		given().contentType(ContentType.JSON).body(REFUEL)
				.post("/api/bill/{carId}/fuel/{billId}", carId, refuelId).then().statusCode(404);
		given().get("/api/attachment/{carId}/bill/{billId}", carId, refuelId).then().statusCode(404);
		// nor the other entries
		given().get("/api/bill/{carId}/all", carId).then().statusCode(200).body("total", is(1));
		given().contentType(ContentType.JSON)
				.body(Map.of("total", 12, "vatRate", 19, "date", "2026-10-01", "description", "Car wash"))
				.put("/api/bill/{carId}/miscellaneous", carId).then().statusCode(404);
	}

	@Test
	void shouldChangeTheOwnRefuelAndAttachTheReceipt()
	{
		share(CarRole.DRIVER);
		refuel().statusCode(anyOf(is(200), is(201), is(202)));
		int own = given().get("/api/bill/{carId}/fuel/all", carId).then().extract().path("data[0].id");

		given().contentType(ContentType.JSON).body(Map.of("date", "2026-10-02", "total", 72, "vatRate", 19,
				"distance", 600, "unit", 41, "pricePerUnit", 1.75))
				.post("/api/bill/{carId}/fuel/{billId}", carId, own).then().statusCode(200);
		given().multiPart("file", "receipt.pdf", "%PDF-1.7\n%%EOF".getBytes(), "application/pdf")
				.post("/api/attachment/{carId}/bill/{billId}", carId, own).then().statusCode(201);
		given().get("/api/attachment/{carId}/bill/{billId}", carId, own).then().statusCode(200)
				.body("size()", is(1));
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldShowTheOwnerWhoCreatedTheEntries()
	{
		share(CarRole.DRIVER);
		QuarkusTransaction.requiringNew().run(() -> fuelBillFactory.create(
				new FuelBillInput(LocalDate.of(2026, 10, 2), BigDecimal.valueOf(70), 19, BigDecimal.valueOf(600),
						BigDecimal.valueOf(40), BigDecimal.valueOf(1.75), null),
				carId, "paul"));

		given().get("/api/bill/{carId}/fuel/all", carId).then().statusCode(200)
				.body("total", is(2))
				.body("data.createdBy", contains("paul", "peter"));
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldKeepTheEntriesOfADeletedAccount()
	{
		share(CarRole.DRIVER);
		QuarkusTransaction.requiringNew().run(() -> fuelBillFactory.create(
				new FuelBillInput(LocalDate.of(2026, 10, 2), BigDecimal.valueOf(70), 19, BigDecimal.valueOf(600),
						BigDecimal.valueOf(40), BigDecimal.valueOf(1.75), null),
				carId, "paul"));

		QuarkusTransaction.requiringNew().run(() -> accountRepository.delete("userId", "paul"));

		given().get("/api/bill/{carId}/fuel/all", carId).then().statusCode(200)
				.body("total", is(2))
				.body("data[0].createdBy", nullValue())
				.body("data[1].createdBy", is("peter"));
	}

	@Test
	void shouldDriveTheOwnTrips()
	{
		share(CarRole.DRIVER);
		UUID own = UUID.randomUUID();

		start(own, 10_000, START).statusCode(201);
		given().get("/api/trip/{carId}/current", carId).then().statusCode(200)
				.body("open.id", is(own.toString()))
				.body("open.driver", is("paul"));
		finish(own, 10_042, START.plusHours(1)).statusCode(200).body("distance", is(42));

		given().get("/api/trip/{carId}", carId).then().statusCode(200)
				.body("total", is(1))
				.body("data[0].driver", is("paul"));
		// deleting stays with the owner and the editors
		given().contentType(ContentType.JSON).body(Map.of("reason", "Wrong"))
				.post("/api/trip/{carId}/{id}/delete", carId, own).then().statusCode(404);
	}

	@Test
	void shouldNotSeeTheTripsOfOthers()
	{
		share(CarRole.DRIVER);
		peterDrives();

		// the open trip of peter isn't shown, starting answers that one is open
		given().get("/api/trip/{carId}/current", carId).then().statusCode(200).body("open", nullValue());
		start(UUID.randomUUID(), 10_100, START.plusHours(2)).statusCode(409);

		given().get("/api/trip/{carId}", carId).then().statusCode(200).body("total", is(0));
		given().get("/api/trip/{carId}/{id}", carId, tripId).then().statusCode(404);
		finish(tripId, 10_050, START.plusHours(1)).statusCode(404);
	}

	@Test
	void shouldNotLetAViewerDrive()
	{
		share(CarRole.VIEWER);

		refuel().statusCode(404);
		start(UUID.randomUUID(), 10_000, START).statusCode(404);
		// but see everything
		given().get("/api/bill/{carId}/fuel/{billId}", carId, refuelId).then().statusCode(200)
				.body("createdBy", is("peter"));
	}

	@Test
	void shouldLetAnEditorFinishTheTripOfADriver()
	{
		share(CarRole.EDITOR);
		peterDrives();

		given().get("/api/trip/{carId}/current", carId).then().statusCode(200)
				.body("open.driver", is("peter"));
		finish(tripId, 10_050, START.plusHours(1)).statusCode(200);
	}

	@Test
	void shouldLetTheOwnerFinishTheTripOfADriverWhoseAccessEnded()
	{
		share(CarRole.DRIVER);
		UUID own = UUID.randomUUID();
		start(own, 10_000, START).statusCode(201);

		// the end of the access passed while the trip is open
		QuarkusTransaction.requiringNew().run(() -> carAccessRepository.findAll().firstResult()
				.setValidUntil(OffsetDateTime.now().minusMinutes(1)));

		given().get("/api/car/my").then().statusCode(200).body("size()", is(0));
		// e.g. sent from the queue of the device after the end
		finish(own, 10_042, START.plusHours(1)).statusCode(404);
		refuel().statusCode(404);

		Trip finished = tripService.finish(carId, own,
				new TripFinishInput(10_042, START.plusHours(1), null, null, null, false), "peter");
		assertEquals("paul", finished.driver());
		assertEquals(42, finished.distance());
	}
}
