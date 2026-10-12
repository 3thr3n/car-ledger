package de.codeflowwizardry.carledger.rest.car.reminder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.MaintenanceBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.maintenance.MaintenanceBillInput;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestHTTPEndpoint(ReminderResource.class)
@TestSecurity(user = "peter", roles = {
		"user"
})
class ReminderResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	MaintenanceBillFactory maintenanceBillFactory;

	long carId;
	long otherCarId;

	final LocalDate today = LocalDate.now();

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();
		carId = car(account("peter"), "Peters car", 50_000);
		otherCarId = car(account("paul"), "Pauls car", 10_000);
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

	private long car(AccountEntity account, String name, int odometer)
	{
		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName(name);
		car.setOdometer(odometer);
		carRepository.persist(car);
		return car.getId();
	}

	private int add(long car, Map<String, Object> input)
	{
		return given()
				.contentType(ContentType.JSON)
				.pathParam("carId", car)
				.body(input)
				.when()
				.put()
				.then()
				.statusCode(202)
				.extract()
				.path("id");
	}

	@Test
	void shouldAddAndListByUrgency()
	{
		add(carId, Map.of("title", "Inspection", "dueDate", today.plusMonths(6).toString()));
		add(carId, Map.of("title", "Oil change", "dueOdometer", 50_500));
		add(carId, Map.of("title", "Tires", "dueDate", today.minusDays(3).toString()));

		given()
				.pathParam("carId", carId)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("title", contains("Tires", "Oil change", "Inspection"))
				.body("status", contains("OVERDUE", "DUE_SOON", "UPCOMING"))
				.body("[0].daysLeft", is(-3))
				.body("[1].kmLeft", is(500))
				.body("[1].odometer", is(50_000))
				.body("[1].carName", is("Peters car"));
	}

	@Test
	void shouldListOnlyDueOnesOfAllCars()
	{
		add(carId, Map.of("title", "Inspection", "dueDate", today.plusMonths(6).toString()));
		add(carId, Map.of("title", "Vignette", "dueDate", today.plusDays(10).toString()));

		given()
				.pathParam("carId", -1)
				.queryParam("onlyDue", true)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("title", contains("Vignette"));
	}

	@Test
	void shouldUseTheHighestMaintenanceOdometer()
	{
		add(carId, Map.of("title", "Oil change", "dueOdometer", 52_000));
		maintenanceBillFactory.create(new MaintenanceBillInput(BigDecimal.TEN, 19, today, 51_200, null, null,
				"Service", "Workshop"), carId, "peter");

		given()
				.pathParam("carId", carId)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("[0].odometer", is(51_200))
				.body("[0].kmLeft", is(800))
				.body("[0].status", is("DUE_SOON"));
	}

	@Test
	void shouldRejectInvalidInput()
	{
		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(Map.of("title", " ", "intervalKm", 15_000))
				.when()
				.put()
				.then()
				.statusCode(400)
				.body("$", hasItems("Title must be set!", "Due date or due odometer must be set!",
						"Interval in km needs a due odometer!"));
	}

	@Test
	void shouldUpdate()
	{
		int id = add(carId, Map.of("title", "Inspection", "dueDate", today.plusMonths(6).toString()));

		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(Map.of("title", "Main inspection", "note", "TÜV", "dueDate", today.plusMonths(7).toString(),
						"intervalMonths", 24))
				.when()
				.post("{id}", id)
				.then()
				.statusCode(200)
				.body("title", is("Main inspection"))
				.body("note", is("TÜV"))
				.body("intervalMonths", is(24))
				.body("dueDate", is(today.plusMonths(7).toString()));
	}

	@Test
	void shouldRescheduleRepeatingWhenDone()
	{
		int id = add(carId, Map.of("title", "Service", "dueDate", today.minusDays(5).toString(), "dueOdometer",
				49_000, "intervalMonths", 12, "intervalKm", 15_000));

		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(Map.of("date", "2026-03-01"))
				.when()
				.post("{id}/done", id)
				.then()
				.statusCode(200)
				.body("dueDate", is("2027-03-01"))
				// from the current odometer of the car
				.body("dueOdometer", is(65_000));

		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(Map.of("date", "2026-03-01", "odometer", 50_200))
				.when()
				.post("{id}/done", id)
				.then()
				.statusCode(200)
				.body("dueOdometer", is(65_200));
	}

	@Test
	void shouldDropTheDueOdometerWithoutKmInterval()
	{
		int id = add(carId, Map.of("title", "Inspection", "dueDate", today.toString(), "dueOdometer", 60_000,
				"intervalMonths", 24));

		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(Map.of("date", "2026-03-01"))
				.when()
				.post("{id}/done", id)
				.then()
				.statusCode(200)
				.body("dueDate", is("2028-03-01"))
				.body("dueOdometer", nullValue());
	}

	@Test
	void shouldDeleteOneOffWhenDone()
	{
		int id = add(carId, Map.of("title", "Vignette", "dueDate", today.toString()));

		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(Map.of())
				.when()
				.post("{id}/done", id)
				.then()
				.statusCode(204);

		given().pathParam("carId", carId).when().get().then().body("size()", is(0));
	}

	@Test
	void shouldDelete()
	{
		int id = add(carId, Map.of("title", "Vignette", "dueDate", today.toString()));

		given().pathParam("carId", carId).when().delete("{id}", id).then().statusCode(204);
		given().pathParam("carId", carId).when().delete("{id}", id).then().statusCode(404);
	}

	@Test
	void shouldNotTouchRemindersOfOtherUsers()
	{
		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", otherCarId)
				.body(Map.of("title", "Vignette", "dueDate", today.toString()))
				.when()
				.put()
				.then()
				.statusCode(404);

		int id = given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(Map.of("title", "Vignette", "dueDate", today.toString()))
				.when()
				.put()
				.then()
				.extract()
				.path("id");

		// the id of peters reminder below the car of paul
		given().pathParam("carId", otherCarId).when().delete("{id}", id).then().statusCode(404);
		given().pathParam("carId", otherCarId).when().get().then().statusCode(200).body("size()", is(0));
	}

	@Test
	void shouldBeDeletedWithTheCar()
	{
		add(carId, Map.of("title", "Vignette", "dueDate", today.toString()));

		QuarkusTransaction.requiringNew().run(() -> carRepository.deleteForUser(carId, "peter"));

		given().pathParam("carId", -1).when().get().then().body("size()", is(0));
	}
}
