package de.codeflowwizardry.carledger.rest.car.modification;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.MiscellaneousBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.ModificationRepository;
import de.codeflowwizardry.carledger.rest.car.miscellaneous.MiscellaneousBillInput;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestHTTPEndpoint(ModificationResource.class)
@TestSecurity(user = "peter", roles = {
		"user"
})
class ModificationResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	BillRepository billRepository;

	@Inject
	ModificationRepository modificationRepository;

	@Inject
	MiscellaneousBillFactory miscellaneousBillFactory;

	long carId;
	long otherCarId;

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

	private long bill(long car, String user)
	{
		return miscellaneousBillFactory.create(new MiscellaneousBillInput(BigDecimal.valueOf(1200), 19,
				LocalDate.of(2025, 4, 1), "Rims"), car, user).getBill().getId();
	}

	private static Map<String, Object> rims()
	{
		Map<String, Object> input = new HashMap<>();
		input.put("part", " Rims 8x18 ET45 ");
		input.put("manufacturer", "OZ");
		input.put("approvalType", "PARTS_REPORT");
		input.put("approvalNumber", "KBA 12345");
		input.put("conditions", "Only with 225/40 R18");
		input.put("installedDate", "2025-04-02");
		input.put("installedOdometer", 41_000);
		input.put("notes", " ");
		return input;
	}

	private int add(Map<String, Object> input)
	{
		return given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(input)
				.when()
				.put()
				.then()
				.statusCode(202)
				.extract()
				.path("id");
	}

	@Test
	void shouldAddWithAnOpenInspection()
	{
		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(rims())
				.when()
				.put()
				.then()
				.statusCode(202)
				.body("part", is("Rims 8x18 ET45"))
				.body("notes", nullValue())
				.body("carId", is((int) carId))
				.body("registered", is(false))
				.body("installed", is(true))
				.body("inspectionOpen", is(true));
	}

	@Test
	void shouldListTheInstalledOnesFirstAndFilter()
	{
		Map<String, Object> exhaust = rims();
		exhaust.put("part", "Exhaust");
		exhaust.put("approvalType", "EC_TYPE_APPROVAL");
		exhaust.put("installedDate", "2025-06-01");
		add(exhaust);
		add(rims());
		Map<String, Object> spoiler = rims();
		spoiler.put("part", "Spoiler");
		spoiler.put("approvalType", "ABE");
		spoiler.put("installedDate", "2025-07-01");
		spoiler.put("removedDate", "2025-09-01");
		spoiler.put("removedOdometer", 45_000);
		add(spoiler);

		given().pathParam("carId", carId).when().get().then()
				.statusCode(200)
				.body("part", contains("Exhaust", "Rims 8x18 ET45", "Spoiler"))
				.body("installed", contains(true, true, false))
				.body("inspectionOpen", contains(false, true, false));
		given().pathParam("carId", carId).queryParam("state", "INSTALLED").when().get().then()
				.body("part", contains("Exhaust", "Rims 8x18 ET45"));
		given().pathParam("carId", carId).queryParam("state", "REMOVED").when().get().then()
				.body("part", contains("Spoiler"))
				.body("[0].removedOdometer", is(45_000));
	}

	@Test
	void shouldRejectInvalidInput()
	{
		Map<String, Object> input = rims();
		input.remove("part");
		input.remove("approvalType");
		input.put("conditions", "x".repeat(2001));
		input.put("installedOdometer", -1);
		input.put("removedOdometer", 40_000);

		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(input)
				.when()
				.put()
				.then()
				.statusCode(400)
				.body("", hasItems("Part must be set!", "Approval type must be set!",
						"Conditions must not be longer than 2000 characters!",
						"Installed odometer must be between 0 and 10000000!",
						"Removed odometer needs a removed date!"));

		input = rims();
		input.put("removedDate", "2025-04-01");
		input.put("removedOdometer", 40_000);
		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(input)
				.when()
				.put()
				.then()
				.statusCode(400)
				.body("", containsInAnyOrder("Removed date must not be before the installed date!",
						"Removed odometer must not be below the installed odometer!"));
	}

	@Test
	void shouldUpdateAndRemoveIntoTheHistory()
	{
		int id = add(rims());

		Map<String, Object> input = rims();
		input.put("inspectionDate", "2025-04-10");
		input.put("testingBody", "TÜV Süd");
		input.put("registered", true);
		input.put("removedDate", "2026-03-01");
		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(input)
				.when()
				.post("{id}", id)
				.then()
				.statusCode(200)
				.body("testingBody", is("TÜV Süd"))
				.body("registered", is(true))
				.body("installed", is(false))
				.body("inspectionOpen", is(false));

		given().pathParam("carId", carId).when().get().then()
				.body("size()", is(1))
				.body("[0].removedDate", is("2026-03-01"));
	}

	@Test
	void shouldLinkABillOfTheSameCar()
	{
		long billId = bill(carId, "peter");
		long otherBillId = bill(otherCarId, "paul");

		Map<String, Object> input = rims();
		input.put("billId", otherBillId);
		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(input)
				.when()
				.put()
				.then()
				.statusCode(400)
				.body("", contains("The bill must be an entry of the same car!"));

		input.put("billId", billId);
		add(input);
		given().pathParam("carId", carId).when().get().then().body("[0].billId", is((int) billId));

		// the modification stays without the bill
		QuarkusTransaction.requiringNew().run(() -> billRepository.delete(carId, billId, "peter"));
		given().pathParam("carId", carId).when().get().then()
				.body("size()", is(1))
				.body("[0].billId", nullValue());
	}

	@Test
	void shouldDelete()
	{
		int id = add(rims());

		given().pathParam("carId", carId).when().delete("{id}", id).then().statusCode(204);
		given().pathParam("carId", carId).when().delete("{id}", id).then().statusCode(404);
		given().pathParam("carId", carId).when().get().then().body("size()", is(0));
	}

	@Test
	void shouldNotTouchModificationsOfOtherUsers()
	{
		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", otherCarId)
				.body(rims())
				.when()
				.put()
				.then()
				.statusCode(404);

		int id = add(rims());

		// the id of peters modification below the car of paul
		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", otherCarId)
				.body(rims())
				.when()
				.post("{id}", id)
				.then()
				.statusCode(404);
		given().pathParam("carId", otherCarId).when().delete("{id}", id).then().statusCode(404);
		given().pathParam("carId", otherCarId).when().get().then().statusCode(200).body("size()", is(0));
	}

	@Test
	void shouldBeDeletedWithTheCar()
	{
		add(rims());

		QuarkusTransaction.requiringNew().run(() -> carRepository.deleteForUser(carId, "peter"));

		long left = QuarkusTransaction.requiringNew().call(modificationRepository::count);
		assertEquals(0, left);
	}
}
