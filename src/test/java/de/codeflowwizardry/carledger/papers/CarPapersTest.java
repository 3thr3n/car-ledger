package de.codeflowwizardry.carledger.papers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.Document;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import de.codeflowwizardry.carledger.ModificationService;
import de.codeflowwizardry.carledger.TechnicalInspectionService;
import de.codeflowwizardry.carledger.attachment.AttachmentService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.ApprovalType;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.DocumentKind;
import de.codeflowwizardry.carledger.data.InspectionResult;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.inspection.TechnicalInspectionInput;
import de.codeflowwizardry.carledger.rest.car.modification.ModificationInput;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Peter prints the papers of his Golf for a roadside check and for selling it
 */
@QuarkusTest
@TestSecurity(user = "peter", roles = "user")
class CarPapersTest
{
	/** 1×1 PNG */
	private static final byte[] PNG = Base64.getDecoder().decode(
			"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
	/** the start of a WebP, enough for the type check, nothing to draw */
	private static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

	@Inject
	AccountRepository accountRepository;
	@Inject
	CarRepository carRepository;
	@Inject
	ModificationService modificationService;
	@Inject
	TechnicalInspectionService inspectionService;
	@Inject
	AttachmentService attachmentService;

	long carId;

	@BeforeEach
	void setup()
	{
		carId = createCars();
		long coilovers = modificationService.create(carId, new ModificationInput("Coilovers", "KW",
				ApprovalType.PARTS_REPORT, "TG 4711", "Only with 225/40 R18", LocalDate.of(2025, 5, 2), "TÜV Süd",
				true, LocalDate.of(2025, 4, 28), 78_000, null, null, null, null), "peter").id();
		attachmentService.addToModification(carId, coilovers, "peter", "gutachten.pdf", DocumentKind.APPROVAL,
				pdf("Gutachten page one", "Gutachten page two"));
		attachmentService.addToModification(carId, coilovers, "peter", "front.png", DocumentKind.PHOTO, PNG);
		attachmentService.addToModification(carId, coilovers, "peter", "side.webp", DocumentKind.PHOTO, WEBP);
		modificationService.create(carId, new ModificationInput("Exhaust", null, ApprovalType.SINGLE_APPROVAL, null,
				null, null, null, false, LocalDate.of(2026, 9, 1), 80_000, null, null, null, null), "peter");
		modificationService.create(carId, new ModificationInput("Spoiler", null, ApprovalType.ABE, null, null, null,
				null, false, LocalDate.of(2024, 1, 1), 60_000, LocalDate.of(2025, 1, 1), 70_000, null, null), "peter");

		inspectionService.create(carId, new TechnicalInspectionInput(LocalDate.of(2022, 5, 3), 52_000, "DEKRA",
				InspectionResult.MINOR_DEFECTS, "Wiper blades", LocalDate.of(2024, 5, 1)), "peter");
		long latest = inspectionService.create(carId, new TechnicalInspectionInput(LocalDate.of(2024, 5, 6), 66_000,
				"DEKRA", InspectionResult.NO_DEFECTS, null, LocalDate.of(2026, 5, 1)), "peter").id();
		attachmentService.addToTechnicalInspection(carId, latest, "peter", "hu.pdf", pdf("HU report"));
	}

	@Transactional
	long createCars()
	{
		accountRepository.deleteAll();
		long peters = 0;
		for (String user : new String[] {
				"peter", "paul"
		})
		{
			AccountEntity account = new AccountEntity();
			account.setUserId(user);
			account.setMaxCars(1);
			accountRepository.persist(account);

			CarEntity car = new CarEntity();
			car.setUser(account);
			car.setName("Golf GTI");
			car.setLicensePlate("M-AB 123");
			car.setVin("WVWZZZ1KZ6W000001");
			carRepository.persist(car);
			if (user.equals("peter"))
			{
				peters = car.getId();
			}
		}
		return peters;
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	/** a PDF with a page per text */
	private static byte[] pdf(String... pages)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Document document = new Document(PageSize.A4);
		PdfWriter.getInstance(document, out);
		document.open();
		for (String page : pages)
		{
			document.newPage();
			document.add(new Paragraph(page));
		}
		document.close();
		return out.toByteArray();
	}

	private byte[] export(String query)
	{
		return given()
				.when()
				.get("/api/export/{carId}/papers/pdf" + query, carId)
				.then()
				.statusCode(200)
				.contentType("application/pdf")
				.extract()
				.asByteArray();
	}

	/** the text of each page, from 1 */
	private static String[] pages(byte[] pdf) throws IOException
	{
		PdfReader reader = new PdfReader(pdf);
		PdfTextExtractor extractor = new PdfTextExtractor(reader);
		String[] pages = new String[reader.getNumberOfPages() + 1];
		for (int page = 1; page < pages.length; page++)
		{
			pages[page] = extractor.getTextFromPage(page);
		}
		return pages;
	}

	@Test
	void shouldPutTheInstalledPartsAndTheLatestHuWithTheirFiles() throws IOException
	{
		String[] pages = pages(export("?language=en"));

		String cover = pages[1];
		assertTrue(cover.contains("Modifications and HU"), cover);
		assertTrue(cover.contains("M-AB 123"), cover);
		assertTrue(cover.contains("WVWZZZ1KZ6W000001"), cover);
		assertTrue(cover.contains("Coilovers"), cover);
		assertTrue(cover.contains("TG 4711"), cover);
		assertTrue(cover.contains("Only with 225/40 R18"), cover);
		// the Einzelabnahme of the exhaust is still open
		assertTrue(cover.contains("Open"), cover);
		assertFalse(cover.contains("Spoiler"), cover);
		assertTrue(cover.contains("May 6, 2024"), cover);
		assertTrue(cover.contains("May 2026"), cover);
		assertFalse(cover.contains("Wiper blades"), cover);

		// the cover, the two pages of the Gutachten, the photo, the WebP as a note, the
		// HU report
		assertEquals(7, pages.length);
		assertTrue(pages[2].contains("Coilovers – Gutachten / ABE: gutachten.pdf (page 1 of 2)"), pages[2]);
		assertTrue(pages[2].contains("Gutachten page one"), pages[2]);
		assertTrue(pages[3].contains("Gutachten page two"), pages[3]);
		assertTrue(pages[4].contains("Coilovers – Photo: front.png"), pages[4]);
		assertTrue(pages[5].contains("can't be included (image/webp)"), pages[5]);
		assertTrue(pages[6].contains("HU of May 6, 2024: hu.pdf"), pages[6]);
		assertTrue(pages[6].contains("HU report"), pages[6]);
	}

	@Test
	void shouldPutTheHistoryForSelling() throws IOException
	{
		String cover = pages(export("?history=true&language=de"))[1];

		assertTrue(cover.contains("Umbauten und HU"), cover);
		assertTrue(cover.contains("Spoiler"), cover);
		assertTrue(cover.contains("Ausgebaut"), cover);
		assertTrue(cover.contains("Mängel: Wiper blades"), cover);
		assertTrue(cover.contains("Geringe Mängel"), cover);
	}

	@Test
	void shouldNameTheFileAfterTheCar()
	{
		given()
				.when()
				.get("/api/export/{carId}/papers/pdf", carId)
				.then()
				.statusCode(200)
				.header("Content-Disposition",
						is("attachment; filename=\"golf-gti-papers-" + LocalDate.now() + ".pdf\""));
	}

	@Test
	void shouldRejectAnUnknownLanguage()
	{
		given()
				.when()
				.get("/api/export/{carId}/papers/pdf?language=fr", carId)
				.then()
				.statusCode(400)
				.body("", contains("Language must be one of [de, en]!"));
	}

	@Test
	@TestSecurity(user = "paul", roles = "user")
	void shouldNotExportTheCarOfAnotherUser()
	{
		given().when().get("/api/export/{carId}/papers/pdf", carId).then().statusCode(404);
	}
}
