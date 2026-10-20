package de.codeflowwizardry.carledger.tripexport;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import de.codeflowwizardry.carledger.TripService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.TripType;
import de.codeflowwizardry.carledger.data.UnitSystem;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.trip.TripDeleteInput;
import de.codeflowwizardry.carledger.rest.car.trip.TripPastInput;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Peter's Golf at 10000 km: a business trip and a deleted private trip in March
 * 2025, a commute in the first minutes of 2026 in Berlin, which is still 2025
 * in UTC
 */
@QuarkusTest
@TestSecurity(user = "peter", roles = "user")
class TripExportTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	TripService tripService;

	long carId;

	@BeforeEach
	void setup()
	{
		carId = QuarkusTransaction.requiringNew().call(this::createCar);
		trip(TripType.BUSINESS, 10_000, "2025-03-02T08:00:00+01:00", 10_120, "2025-03-02T10:30:00+01:00",
				"Munich", "Customer visit", "ACME");
		UUID deleted = trip(TripType.PRIVATE, 10_120, "2025-03-05T17:00:00+01:00", 10_150,
				"2025-03-05T18:00:00+01:00", null, null, null);
		tripService.delete(carId, deleted, new TripDeleteInput("Entered twice"), "peter");
		trip(TripType.COMMUTE, 10_120, "2026-01-01T00:30:00+01:00", 10_140, "2026-01-01T01:00:00+01:00", null,
				null, null);
	}

	long createCar()
	{
		AccountEntity peter = account("peter");
		account("bob");
		CarEntity car = new CarEntity();
		car.setUser(peter);
		car.setName("My Golf");
		car.setLicensePlate("B-CL 123");
		car.setManufactureYear(2020);
		car.setOdometer(10_000);
		car.setOdometerDate(LocalDate.of(2025, 1, 1));
		car.setTripLog(true);
		carRepository.persist(car);
		return car.getId();
	}

	private AccountEntity account(String name)
	{
		AccountEntity account = new AccountEntity();
		account.setUserId(name);
		account.setMaxCars(1);
		accountRepository.persist(account);
		return account;
	}

	private UUID trip(TripType type, int from, String start, int to, String end, String destination,
			String purpose, String partner)
	{
		UUID id = UUID.randomUUID();
		tripService.addPast(carId, id, new TripPastInput(type, from, OffsetDateTime.parse(start), to,
				OffsetDateTime.parse(end), destination, purpose, partner), "peter");
		return id;
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	private ValidatableResponse export(String format, String query)
	{
		return given().when().get("/api/export/" + carId + "/trips/" + format + query).then();
	}

	@Test
	void shouldExportTheTripsAsCsvInTheZone()
	{
		String csv = export("csv", "?year=2025&zone=Europe/Berlin")
				.statusCode(200)
				.contentType("text/csv; charset=UTF-8")
				.header("Content-Disposition", "attachment; filename=\"my-golf-trips-2025.csv\"")
				.extract().asString();

		List<String> lines = csv.lines().toList();
		assertEquals(String.join(",", TripCsvWriter.HEADER), lines.get(0));
		// the commute is in 2026 in Berlin
		assertEquals(3, lines.size(), csv);
		assertTrue(lines.get(1).startsWith("2025-03-02T08:00+01:00,2025-03-02T10:30+01:00,BUSINESS,10000,10120,120,"
				+ "Munich,Customer visit,ACME,peter,true,"), lines.get(1));
		assertTrue(lines.get(2).endsWith(",peter,Entered twice"), lines.get(2));

		// in UTC it starts in 2025
		assertEquals(4, export("csv", "?year=2025&zone=UTC").extract().asString().lines().count());
		assertEquals(4, export("csv", "").extract().asString().lines().count());
	}

	@Test
	void shouldExportAPeriod()
	{
		String csv = export("csv", "?from=2025-03-03&to=2025-03-05&zone=Europe/Berlin")
				.statusCode(200)
				.header("Content-Disposition",
						"attachment; filename=\"my-golf-trips-2025-03-03-2025-03-05.csv\"")
				.extract().asString();
		assertEquals(2, csv.lines().count());
		assertTrue(csv.contains("Entered twice"));
	}

	@Test
	void shouldExportTheTripsAsPdf() throws IOException
	{
		byte[] pdf = export("pdf", "?year=2025&zone=Europe/Berlin&language=de")
				.statusCode(200)
				.contentType("application/pdf")
				.header("Content-Disposition", "attachment; filename=\"my-golf-trips-2025.pdf\"")
				.extract().asByteArray();

		String text = text(pdf);
		assertTrue(text.contains("Fahrtenbuch"), text);
		assertTrue(text.contains("B-CL 123"), text);
		assertTrue(text.contains("01.01.2025 – 31.12.2025"), text);
		assertTrue(text.contains("Customer visit"), text);
		assertTrue(text.contains("10.120 km"), text);
		assertTrue(text.contains("Entered twice"), text);
		assertTrue(text.contains("Nachgetragen am"), text);
		// the deleted trip doesn't count
		assertTrue(text.contains("Dienstlich 1 120 km"), text);
		assertTrue(text.contains("Privat 0 0 km"), text);
		assertTrue(text.contains("Gesamt 1 120 km"), text);
	}

	@Test
	void shouldExportThePdfInTheLanguageAndTheUnitsOfTheAccount() throws IOException
	{
		QuarkusTransaction.requiringNew().run(() -> {
			AccountEntity peter = accountRepository.findByIdentifier("peter");
			peter.setUnitSystem(UnitSystem.US);
			peter.setLanguage("EN");
		});

		String text = text(export("pdf", "?zone=Europe/Berlin").statusCode(200).extract().asByteArray());
		assertTrue(text.contains("Trip log"), text);
		assertTrue(text.contains("All trips"), text);
		assertTrue(text.contains("6,288 mi"), text);
		assertTrue(text.contains("Total 2 87 mi"), text);
	}

	@Test
	void shouldRejectAWrongPeriodZoneOrLanguage()
	{
		export("csv", "?year=2025&from=2025-01-01").statusCode(400)
				.body("[0]", is("Either a year or a period can be given!"));
		export("csv", "?from=2025-02-01&to=2025-01-01").statusCode(400);
		export("csv", "?zone=Mars/Olympus").statusCode(400).body("[0]", is("Unknown time zone!"));
		export("pdf", "?language=fr").statusCode(400);
	}

	@Test
	@TestSecurity(user = "bob", roles = "user")
	void shouldNotExportTheTripsOfAnotherUser()
	{
		export("csv", "").statusCode(404);
		export("pdf", "").statusCode(404);
	}

	private static String text(byte[] pdf) throws IOException
	{
		PdfReader reader = new PdfReader(pdf);
		StringBuilder text = new StringBuilder();
		PdfTextExtractor extractor = new PdfTextExtractor(reader);
		for (int page = 1; page <= reader.getNumberOfPages(); page++)
		{
			text.append(extractor.getTextFromPage(page)).append('\n');
		}
		return text.toString().replaceAll("\\s+", " ");
	}
}
