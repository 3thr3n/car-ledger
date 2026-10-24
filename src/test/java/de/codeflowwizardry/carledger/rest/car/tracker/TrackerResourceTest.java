package de.codeflowwizardry.carledger.rest.car.tracker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.tracker.DeviceService;
import de.codeflowwizardry.carledger.tracker.DeviceTokens;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@QuarkusTest
@TestHTTPEndpoint(TrackerResource.class)
@TestSecurity(user = "peter", roles = {
		"user"
})
class TrackerResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	EntityManager entityManager;

	@Inject
	DeviceService deviceService;

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

	private Map<String, Object> add(long car, String name)
	{
		return given()
				.contentType(ContentType.JSON)
				.pathParam("carId", car)
				.body(Map.of("name", name))
				.when()
				.put()
				.then()
				.statusCode(201)
				.extract()
				.jsonPath()
				.getMap("$");
	}

	@SuppressWarnings("unchecked")
	private static long id(Map<String, Object> created)
	{
		return ((Number) ((Map<String, Object>) created.get("device")).get("id")).longValue();
	}

	@Test
	void shouldReturnTheTokenOnlyOnCreate()
	{
		Map<String, Object> created = add(carId, " ESP32 ");
		String token = (String) created.get("token");

		assertEquals("ESP32", ((Map<?, ?>) created.get("device")).get("name"));
		// cl_ and 32 bytes base64url without padding
		assertEquals(46, token.length());
		assertEquals(DeviceTokens.PREFIX, token.substring(0, 3));

		given()
				.pathParam("carId", carId)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("name", contains("ESP32"))
				.body("[0].createdAt", notNullValue())
				.body("[0].lastSeenAt", nullValue())
				.body("[0].revokedAt", nullValue())
				.body("[0]", not(hasKey("token")))
				.body("[0]", not(hasKey("tokenHash")));
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldStoreOnlyTheHashOfTheToken()
	{
		String token = (String) add(carId, "ESP32").get("token");

		List<String> hashes = entityManager.createNativeQuery("select dv_token_hash from device").getResultList();
		assertEquals(List.of(DeviceTokens.hash(token)), hashes);
	}

	@Test
	void shouldListTheNewestFirst()
	{
		add(carId, "First");
		add(carId, "Second");

		given()
				.pathParam("carId", carId)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("name", contains("Second", "First"));
	}

	@Test
	void shouldValidateTheName()
	{
		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(Map.of("name", " "))
				.when()
				.put()
				.then()
				.statusCode(400)
				.body("", contains("Name must be set!"));

		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", carId)
				.body(Map.of("name", "x".repeat(101)))
				.when()
				.put()
				.then()
				.statusCode(400);
	}

	@Test
	void shouldRevokeKeepingTheFirstTime()
	{
		long id = id(add(carId, "ESP32"));

		String revokedAt = given()
				.pathParam("carId", carId)
				.when()
				.post("{id}/revoke", id)
				.then()
				.statusCode(200)
				.body("revokedAt", notNullValue())
				.extract()
				.path("revokedAt");

		String again = given()
				.pathParam("carId", carId)
				.when()
				.post("{id}/revoke", id)
				.then()
				.statusCode(200)
				.extract()
				.path("revokedAt");

		assertTrue(OffsetDateTime.parse(again).isEqual(OffsetDateTime.parse(revokedAt)));
	}

	@Test
	void shouldNotOpenTheTrackersOfAnotherCar()
	{
		given()
				.contentType(ContentType.JSON)
				.pathParam("carId", otherCarId)
				.body(Map.of("name", "ESP32"))
				.when()
				.put()
				.then()
				.statusCode(404);

		given()
				.pathParam("carId", otherCarId)
				.when()
				.get()
				.then()
				.statusCode(200)
				.body("", empty());
	}

	@Test
	void shouldNotRevokeTheTrackerOfAnotherUser()
	{
		long id = deviceService.create(otherCarId, new DeviceInput("ESP32"), "paul").device().id();

		given()
				.pathParam("carId", otherCarId)
				.when()
				.post("{id}/revoke", id)
				.then()
				.statusCode(404);
	}

	@Test
	void shouldNotRevokeATrackerOfAnotherCarOfTheUser()
	{
		long id = id(add(carId, "ESP32"));

		given()
				.pathParam("carId", otherCarId)
				.when()
				.post("{id}/revoke", id)
				.then()
				.statusCode(404);
	}

	@Test
	void shouldRenewTheToken()
	{
		Map<String, Object> created = add(carId, "ESP32");
		String old = (String) created.get("token");
		long id = id(created);

		String token = given()
				.pathParam("carId", carId)
				.when()
				.post("{id}/token", id)
				.then()
				.statusCode(200)
				.body("device.id", is((int) id))
				.body("device.name", is("ESP32"))
				.extract()
				.path("token");

		assertNotEquals(old, token);
		assertTrue(deviceService.authenticate(old).isEmpty());
		assertEquals(id, deviceService.authenticate(token).orElseThrow().deviceId());
	}

	@Test
	void shouldNotRenewTheTokenOfARevokedTracker()
	{
		Map<String, Object> created = add(carId, "ESP32");
		long id = id(created);
		given().pathParam("carId", carId).when().post("{id}/revoke", id).then().statusCode(200);

		given()
				.pathParam("carId", carId)
				.when()
				.post("{id}/token", id)
				.then()
				.statusCode(409);

		assertTrue(deviceService.authenticate((String) created.get("token")).isEmpty());
	}

	@Test
	void shouldNotRenewTheTokenOfAnotherUser()
	{
		long id = deviceService.create(otherCarId, new DeviceInput("ESP32"), "paul").device().id();

		given()
				.pathParam("carId", otherCarId)
				.when()
				.post("{id}/token", id)
				.then()
				.statusCode(404);
	}
}
