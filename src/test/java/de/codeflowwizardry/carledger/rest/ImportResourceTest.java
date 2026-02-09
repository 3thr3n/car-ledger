package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
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
	BillRepository billRepository;

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
	void shouldImportFuelCsv()
	{
		String order = """
				{
					"date": 0,
					"unit": 1,
					"pricePerUnit": 2,
					"estimate": 3,
					"distance": 4
				}
				""";

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/fuel_import_1.csv"), "text/csv")
				.multiPart("order", order)
				.multiPart("billType", "FUEL")
				.multiPart("vat", 19)
				.when()
				.post("/api/import/" + carEntity.getId())
				.then()
				.statusCode(200);

		assertEquals(13, billRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldImportMaintenanceCsv()
	{
		String order = """
				{
					"date": 0,
					"total": 1,
					"odometer": 2,
					"workshop": 3
				}
				""";

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/maintenance_import_1.csv"), "text/csv")
				.multiPart("order", order)
				.multiPart("billType", "MAINTENANCE")
				.multiPart("vat", 19)
				.when()
				.post("/api/import/" + carEntity.getId())
				.then()
				.statusCode(200);

		assertEquals(2, billRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldImportMiscellaneousCsv()
	{
		String order = """
				{
					"date": 0,
					"total": 1,
					"description": 2
				}
				""";

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/miscellaneous_import_1.csv"), "text/csv")
				.multiPart("order", order)
				.multiPart("billType", "MISCELLANEOUS")
				.multiPart("vat", 19)
				.when()
				.post("/api/import/" + carEntity.getId())
				.then()
				.statusCode(200);

		assertEquals(2, billRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldFailWithWrongType()
	{
		String order = """
				{
					"date": 0,
					"total": 1,
					"odometer": 2,
					"workshop": 3
				}
				""";

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/maintenance_import_1.csv"), "text/csv")
				.multiPart("order", order)
				.multiPart("billType", "abc")
				.multiPart("vat", 19)
				.when()
				.post("/api/import/" + carEntity.getId())
				.then()
				.statusCode(400);
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldImportCsvInvalidDate()
	{
		String order = """
				{
					"date": 0,
					"unit": 2,
					"pricePerUnit": 1,
					"estimate": 3,
					"distance": 4
				}
				""";

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/fuel_import_3_invalid.csv"), "text/csv")
				.multiPart("order", order)
				.multiPart("billType", "FUEL")
				.multiPart("vat", 19)
				.when()
				.post("/api/import/" + carEntity.getId())
				.then()
				.statusCode(200)
				.body("errorCount", is(13));

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
					"date": 0,
					"unit": 2,
					"pricePerUnit": 1,
					"estimate": 3,
					"distance": 4
				}
				""";

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/fuel_import_2.csv"), "text/csv")
				.multiPart("order", order)
				.multiPart("billType", "FUEL")
				.multiPart("vat", 19)
				.when()
				.post("/api/import/" + carEntity.getId())
				.then()
				.statusCode(200);

		assertEquals(13, billRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldImportCsvSkipHeader()
	{
		String order = """
				{
					"date": 0,
					"unit": 1,
					"pricePerUnit": 2,
					"estimate": 3,
					"distance": 4
				}
				""";

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/fuel_import_4.csv"), "text/csv")
				.multiPart("order", order)
				.multiPart("billType", "FUEL")
				.multiPart("vat", 19)
				.when()
				.post("/api/import/" + carEntity.getId() + "?skipHeader=true").then().statusCode(200);

		assertEquals(13, billRepository.count());
	}

	@Test
	@TestSecurity(user = "bob", roles = {
			"user"
	})
	void bobShouldNotBeAbleToImportCsvOnPetersCar()
	{
		String order = """
				{
					"date": 0,
					"unit": 1,
					"pricePerUnit": 2,
					"estimate": 3,
					"distance": 4
				}
				""";

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/fuel_import_1.csv"), "text/csv")
				.multiPart("order", order)
				.multiPart("billType", "FUEL")
				.multiPart("vat", 19)
				.when()
				.post("/api/import/" + carEntity.getId()).then().statusCode(400);

		assertEquals(0, billRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldImportCsvTwice()
	{
		String order = """
				{
					"date": 0,
					"unit": 1,
					"pricePerUnit": 2,
					"estimate": 3,
					"distance": 4
				}
				""";

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/fuel_import_1.csv"), "text/csv")
				.multiPart("order", order)
				.multiPart("billType", "FUEL")
				.multiPart("vat", 19)
				.when()
				.post("/api/import/" + carEntity.getId()).then().statusCode(200);

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", getFile("csv/fuel_import_1.csv"), "text/csv")
				.multiPart("order", order)
				.multiPart("billType", "FUEL")
				.multiPart("vat", 19)
				.when()
				.post("/api/import/" + carEntity.getId()).then().statusCode(200);

		assertEquals(13, billRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldFailImporting()
	{
		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.png", getFile("white_with_dot.png"), "image/png")
				.when()
				.post("/api/import/" + carEntity.getId())
				.then()
				.statusCode(400)
				.body(is("Vat rate cannot be null!"));

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.png", getFile("white_with_dot.png"), "image/png")
				.multiPart("vat", 19)
				.multiPart("billType", "FUEL")
				.when()
				.post("/api/import/" + carEntity.getId())
				.then()
				.statusCode(500);

		String order = """
				{
					"date": 0,
					"unit": 1,
					"pricePerUnit": 2,
					"estimate": 3,
					"distance": 4
				}
				""";

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.png", getFile("white_with_dot.png"), "image/png")
				.multiPart("order", order)
				.multiPart("billType", "FUEL")
				.multiPart("vat", 19)
				.when()
				.post("/api/import/" + carEntity.getId())
				.then()
				.statusCode(500);

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.png", getFile("white_with_dot.png"), "image/png")
				.multiPart("order", order)
				.multiPart("vat", 19)
				.when()
				.post("/api/import/" + carEntity.getId())
				.then()
				.statusCode(400)
				.body(is("Type not found!"));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldFetchFields()
	{
		given()
				.queryParam("importType", "FUEL")
				.when()
				.get("/api/import/" + carEntity.getId() + "/fields")
				.then()
				.statusCode(200);

		given()
				.queryParam("importType", "MAINTENANCE")
				.when()
				.get("/api/import/" + carEntity.getId() + "/fields")
				.then()
				.statusCode(200);

		given()
				.queryParam("importType", "MISCELLANEOUS")
				.when()
				.get("/api/import/" + carEntity.getId() + "/fields")
				.then()
				.statusCode(200);
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
