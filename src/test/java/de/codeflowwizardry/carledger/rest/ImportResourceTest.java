package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.FuelBillRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class ImportResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	FuelBillRepository billRepository;

	CarEntity carEntity;

	@BeforeEach
	@Transactional
	void setup()
	{
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setMaxCars(1);
		accountEntity.setUserId("bob");
		accountRepository.persist(accountEntity);

		setupPeter();
	}

	private void setupPeter()
	{
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setMaxCars(1);
		accountEntity.setUserId("peter");
		accountRepository.persist(accountEntity);

		carEntity = new CarEntity();
		carEntity.setUser(accountEntity);
		carEntity.setName("Neat car");
		carRepository.persist(carEntity);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldImportCsv()
	{
		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_1.csv"), "text/csv").when()
				.post("/api/import/" + carEntity.getId()).then().statusCode(202);

		assertEquals(13, billRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldImportCsvInvalidDate()
	{
		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_3_invalid.csv"), "text/csv").when()
				.post("/api/import/" + carEntity.getId()).then().statusCode(500);

		assertEquals(0, billRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
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

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_2.csv"), "text/csv").multiPart("order", order)
				.when().post("/api/import/" + carEntity.getId()).then().statusCode(202);

		assertEquals(13, billRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldImportCsvSkipHeader()
	{
		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_4.csv"), "text/csv").when()
				.post("/api/import/" + carEntity.getId() + "?skipHeader=true").then().statusCode(202);

		assertEquals(13, billRepository.count());
	}

	@Test
	@TestSecurity(user = "bob", roles = {
			"user"
	})
	void bobShouldNotBeAbleToImportCsvOnPetersCar()
	{
		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_1.csv"), "text/csv").when()
				.post("/api/import/" + carEntity.getId()).then().statusCode(400);

		assertEquals(0, billRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldImportCsvTwice()
	{
		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_1.csv"), "text/csv").when()
				.post("/api/import/" + carEntity.getId()).then().statusCode(202);

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/import_1.csv"), "text/csv").when()
				.post("/api/import/" + carEntity.getId()).then().statusCode(202);

		assertEquals(13, billRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldFailImporting()
	{
		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.png", getFile("white_with_dot.png"), "image/png").when()
				.post("/api/import/" + carEntity.getId()).then().statusCode(500);
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
