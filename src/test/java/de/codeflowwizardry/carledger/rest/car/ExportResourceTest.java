package de.codeflowwizardry.carledger.rest.car;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class ExportResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	long carId;

	@BeforeEach
	@Transactional
	void setup()
	{
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setMaxCars(1);
		accountEntity.setUserId("peter");
		accountRepository.persist(accountEntity);

		CarEntity carEntity = new CarEntity();
		carEntity.setUser(accountEntity);
		carEntity.setName("Neat Car");
		carRepository.persist(carEntity);
		carId = carEntity.getId();

		AccountEntity other = new AccountEntity();
		other.setMaxCars(1);
		other.setUserId("bob");
		accountRepository.persist(other);
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldExportInTheFormatOfTheImport()
	{
		importCsv(getFile("csv/fuel_import_1.csv"), "FUEL", """
				{ "date": 0, "unit": 1, "pricePerUnit": 2, "distance": 3, "estimateConsumption": 4 }
				""", false)
				.body("successCount", is(13));

		String csv = export("FUEL", null)
				.header("Content-Disposition", "attachment; filename=\"neat-car-fuel.csv\"")
				.extract()
				.asString();

		List<String> lines = csv.lines().toList();
		assertEquals(14, lines.size());
		assertEquals("date,total,vatRate,unit,pricePerUnit,distance,estimateConsumption", lines.get(0));
		// the oldest first, the total is calculated by the import
		assertEquals("2024-04-27,80.16,19,43.12,185.90,529.50,8.70", lines.get(1));

		// imported again with the header as mapping, every row exists already
		importCsv(new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "FUEL", """
				{ "date": 0, "total": 1, "vatRate": 2, "unit": 3, "pricePerUnit": 4, "distance": 5,
				  "estimateConsumption": 6 }
				""", true)
				.body("successCount", is(0))
				.body("skippedCount", is(13))
				.body("errorCount", is(0));
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldExportTheChargesInTheFormatOfTheImport()
	{
		// the location in any case, or none
		importCsv(new java.io.ByteArrayInputStream("""
				2026-03-01,40,30,250,home,80
				2026-03-12,25.5,"59,0",120,Public,
				2026-03-20,8,32,,,
				""".getBytes(StandardCharsets.UTF_8)), "CHARGING", """
				{ "date": 0, "unit": 1, "pricePerUnit": 2, "distance": 3, "location": 4, "stateOfCharge": 5 }
				""", false)
				.body("successCount", is(3))
				.body("errorCount", is(0));

		String csv = export("CHARGING", null)
				.header("Content-Disposition", "attachment; filename=\"neat-car-charging.csv\"")
				.extract()
				.asString();

		assertEquals("""
				date,total,vatRate,unit,pricePerUnit,distance,location,stateOfCharge
				2026-03-01,12.00,19,40.00,30.00,250.00,HOME,80
				2026-03-12,15.05,19,25.50,59.00,120.00,PUBLIC,
				2026-03-20,2.56,19,8.00,32.00,0.00,,
				""", csv);

		// imported again with the header as mapping, every row exists already
		importCsv(new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "CHARGING", """
				{ "date": 0, "total": 1, "vatRate": 2, "unit": 3, "pricePerUnit": 4, "distance": 5,
				  "location": 6, "stateOfCharge": 7 }
				""", true)
				.body("successCount", is(0))
				.body("skippedCount", is(3))
				.body("errorCount", is(0));
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldQuoteTextsAndFilterByYear()
	{
		importCsv(new java.io.ByteArrayInputStream("""
				2024-03-01,99.90,"Oil change, filter"
				2025-03-01,10,Car wash
				""".getBytes(StandardCharsets.UTF_8)), "MISCELLANEOUS", """
				{ "date": 0, "total": 1, "description": 2 }
				""", false)
				.body("successCount", is(2));

		assertEquals("""
				date,total,vatRate,description
				2024-03-01,99.90,19,"Oil change, filter"
				""", export("MISCELLANEOUS", 2024).extract().asString());

		export("MAINTENANCE", null)
				.body(is("date,total,vatRate,workshop,description,odometer,laborCost,partsCost\n"));
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldRejectRecurringAndMissingType()
	{
		given().pathParam("carId", carId)
				.queryParam("type", "RECURRING")
				.get("/api/export/{carId}")
				.then()
				.statusCode(400)
				.body(containsString("FUEL, CHARGING, MAINTENANCE, MISCELLANEOUS"));

		given().pathParam("carId", carId)
				.get("/api/export/{carId}")
				.then()
				.statusCode(400);
	}

	@Test
	@TestSecurity(user = "bob", roles = "user")
	void shouldNotExportTheCarOfAnotherUser()
	{
		given().pathParam("carId", carId)
				.queryParam("type", "FUEL")
				.get("/api/export/{carId}")
				.then()
				.statusCode(404);
	}

	private io.restassured.response.ValidatableResponse export(String type, Integer year)
	{
		var request = given().pathParam("carId", carId).queryParam("type", type);
		if (year != null)
		{
			request.queryParam("year", year);
		}
		return request.get("/api/export/{carId}")
				.then()
				.statusCode(200)
				.contentType(containsString("text/csv"));
	}

	private io.restassured.response.ValidatableResponse importCsv(InputStream csv, String type, String order,
			boolean skipHeader)
	{
		return given().contentType(ContentType.MULTIPART)
				.multiPart("file", "import.csv", csv, "text/csv")
				.multiPart("order", order)
				.multiPart("billType", type)
				.multiPart("vat", 19)
				.queryParam("skipHeader", skipHeader)
				.post("/api/import/" + carId)
				.then()
				.statusCode(200);
	}

	private InputStream getFile(String filename)
	{
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
	}
}
