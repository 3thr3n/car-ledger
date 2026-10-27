package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Inputs the database can't store or that are too large answer 400, not 500
 */
@QuarkusTest
class InputLimitsTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	long carId;

	@TestHTTPResource
	URL url;

	@BeforeEach
	@Transactional
	void setup()
	{
		AccountEntity account = new AccountEntity();
		account.setMaxCars(2);
		account.setUserId("alice");
		accountRepository.persist(account);

		CarEntity car = new CarEntity();
		car.setUser(account);
		car.setName("Golf");
		carRepository.persist(car);
		carId = car.getId();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldLimitTheTexts()
	{
		given().contentType(ContentType.JSON)
				.body(Map.of("name", "x".repeat(101), "year", 2020))
				.post("/api/car/my/{id}", carId)
				.then().statusCode(400).body("$", hasItem("Name must not be longer than 100 characters!"));

		given().contentType(ContentType.JSON)
				.body(Map.of("date", "2026-10-01", "vatRate", 19, "total", 100, "workshop", "x".repeat(101),
						"description", "x".repeat(1001)))
				.put("/api/bill/{carId}/maintenance", carId)
				.then().statusCode(400)
				.body("$", hasItem("Workshop must not be longer than 100 characters!"))
				.body("$", hasItem("Description must not be longer than 1000 characters!"));

		given().contentType(ContentType.JSON)
				.body(Map.of("date", "2026-10-01", "vatRate", 19, "total", 10, "description", "x".repeat(1001)))
				.put("/api/bill/{carId}/miscellaneous", carId)
				.then().statusCode(400).body("$", hasItem("Description must not be longer than 1000 characters!"));

		given().contentType(ContentType.JSON)
				.body(Map.of("name", "x".repeat(101), "category", "INSURANCE", "billInterval", "ANNUALLY",
						"startDate", "2026-01-01", "amount", 300))
				.put("/api/bill/{carId}/recurring", carId)
				.then().statusCode(400).body("$", hasItem("Name must not be longer than 100 characters!"));
	}

	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldRejectANulCharacter()
	{
		given().contentType(ContentType.JSON)
				.body("{\"date\":\"2026-10-01\",\"vatRate\":19,\"total\":10,\"description\":\"a\\u0000b\"}")
				.put("/api/bill/{carId}/miscellaneous", carId)
				.then().statusCode(400);

		given().urlEncodingEnabled(false)
				.get("/api/bill/" + carId + "/miscellaneous/all?sort=%00")
				.then().statusCode(400).body(containsString("NUL"));

		assertTrue(NulBytes.inUri("/api/admin/accounts?search=%00"));
		assertTrue(NulBytes.inUri("/api/x%00y"));
		assertFalse(NulBytes.inUri("/api/admin/accounts?search=100%25"));
	}

	@Test
	@TestSecurity(user = "alice", roles = "user")
	void shouldAnswer400ForABrokenUpload() throws Exception
	{
		// RestAssured sends only valid multipart bodies
		try (HttpClient client = HttpClient.newHttpClient())
		{
			// without its end
			assertUpload(client,
					"--xyz\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.pdf\"\r\n\r\nabc");
			// a part without headers
			assertUpload(client, "--xyz\r\nno header\r\n\r\nabc\r\n--xyz--\r\n");
		}
	}

	private void assertUpload(HttpClient client, String body) throws Exception
	{
		HttpRequest request = HttpRequest.newBuilder(url.toURI().resolve("/api/car/my/" + carId + "/picture"))
				.header("Content-Type", "multipart/form-data; boundary=xyz")
				.PUT(HttpRequest.BodyPublishers.ofString(body))
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(400, response.statusCode(), response.body());
	}
}
