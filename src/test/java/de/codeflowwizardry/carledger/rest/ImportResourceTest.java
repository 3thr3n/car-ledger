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
		setupPeter();
	}

	private void setupPeter()
	{
		Account account = new Account();
		account.setMaxCars(1);
		account.setUserId("peter");
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
				.cookie(cookie)
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
				.cookie(cookie)
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
				.cookie(cookie)
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
				.cookie(cookie)
				.contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_4.csv"), "text/csv")
				.when()
				.post("/api/import/" + car.getId() + "?skipHeader=true")
				.then()
				.statusCode(202);

		assertEquals(13, billRepository.count());
	}

	@Test
	void bobShouldNotBeAbleToImportCsvOnPetersCar()
	{
		given()
				.cookie(cookie)
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
				.cookie(cookie)
				.contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_1.csv"), "text/csv")
				.when()
				.post("/api/import/" + car.getId()).then().statusCode(202);

		given()
				.cookie(cookie)
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
				.cookie(cookie)
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
