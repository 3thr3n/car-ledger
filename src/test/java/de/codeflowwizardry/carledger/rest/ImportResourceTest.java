package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.Car;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ImportResourceTest extends AbstractResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	BillRepository billRepository;

	Car car;

	@BeforeEach
	@Transactional
	void setup()
	{
		setupBob();
	}

	private void setupBob()
	{
		Optional<Account> optionalAccount = accountRepository.findByIdentifier("bob");
		if (optionalAccount.isEmpty()) {
			return;
		}

		Account account = optionalAccount.get();
		account.setMaxCars(2);
		accountRepository.persist(account);

		car = new Car();
		car.setUser(account);
		car.setDescription("Neat car");
		carRepository.persist(car);
	}

	@Test
	void shouldImportCsv()
	{
		given()
				.cookie(bobCookie)
				.contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_1.csv"), "text/csv")
				.when()
				.post("/api/import/" + car.getId())
				.then()
				.statusCode(202);

		assertEquals(13, billRepository.count());
	}

	@Test
	void shouldImportCsvInvalidDate()
	{
		given()
				.cookie(bobCookie)
				.contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_3_invalid.csv"), "text/csv")
				.when()
				.post("/api/import/" + car.getId())
				.then()
				.statusCode(500);

		assertEquals(0, billRepository.count());
	}

	@Test
	void shouldImportCsvWithDifferentOrder()
	{
		String order = """
				{
					"day": 0,
					"unit": 2,
					"pricePerUnit": 1,
					"estimate": 3,
					"distance": 4
				}
				""";

		given()
				.cookie(bobCookie)
				.contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_2.csv"), "text/csv")
				.multiPart("order", order)
				.when()
				.post("/api/import/" + car.getId())
				.then()
				.statusCode(202);

		assertEquals(13, billRepository.count());
	}

	@Test
	void shouldImportCsvSkipHeader()
	{
		given()
				.cookie(bobCookie)
				.contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_4.csv"), "text/csv")
				.when()
				.post("/api/import/" + car.getId() + "?skipHeader=true")
				.then()
				.statusCode(202);

		assertEquals(13, billRepository.count());
	}

	@Test
	void aliceShouldNotBeAbleToImportCsvOnBobsCar()
	{
		given()
				.cookie(aliceCookie)
				.contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_1.csv"), "text/csv")
				.when()
				.post("api/import/" + car.getId())
				.then()
				.statusCode(400);

		assertEquals(0, billRepository.count());
	}

	@Test
	void shouldImportCsvTwice()
	{
		given()
				.cookie(bobCookie)
				.contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_1.csv"), "text/csv")
				.when()
				.post("/api/import/" + car.getId()).then().statusCode(202);

		given()
				.cookie(bobCookie)
				.contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_1.csv"), "text/csv")
				.when()
				.post("/api/import/" + car.getId())
				.then().
				statusCode(202);

		assertEquals(13, billRepository.count());
	}

	@Test
	void shouldFailImporting()
	{
		given()
				.cookie(bobCookie)
				.contentType(ContentType.MULTIPART)
				.multiPart("file", "import.png", getFile("white_with_dot.png"), "image/png")
				.when()
				.post("/api/import/" + car.getId())
				.then()
				.statusCode(500);
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		billRepository.deleteAll();
		carRepository.deleteAll();
		accountRepository.deleteAll();
	}

	private InputStream getFile(String filename)
	{
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
	}
}
