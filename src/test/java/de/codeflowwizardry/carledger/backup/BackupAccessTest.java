package de.codeflowwizardry.carledger.backup;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarAccessEntity;
import de.codeflowwizardry.carledger.data.CarAccessState;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.CarRole;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccessRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/**
 * The accesses of the car of peter in his backup: restored as invitations, a
 * backup gives no access by itself; the shared car is never in the backup of
 * paul
 */
@QuarkusTest
@TestSecurity(user = "peter", roles = "user")
class BackupAccessTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	CarAccessRepository carAccessRepository;

	@Inject
	ObjectMapper mapper;

	long carId;

	@BeforeEach
	void setup()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			AccountEntity peter = account("peter");
			CarEntity car = new CarEntity();
			car.setUser(peter);
			car.setName("Golf");
			car.setManufactureYear(2020);
			car.setPurchaseDate(LocalDate.of(2020, 5, 1));
			carRepository.persist(car);
			carId = car.getId();

			carAccessRepository.persist(new CarAccessEntity(car, account("paul"), CarRole.VIEWER, peter));
			CarAccessEntity newbie = CarAccessEntity.invite(car, null, "newbie@example.com", CarRole.DRIVER, peter);
			newbie.setValidUntil(OffsetDateTime.now().plusDays(14));
			carAccessRepository.persist(newbie);
			// history, not restored
			CarAccessEntity declined = CarAccessEntity.invite(car, account("mary"), null, CarRole.EDITOR, peter);
			declined.setState(CarAccessState.DECLINED);
			carAccessRepository.persist(declined);
			CarAccessEntity ended = new CarAccessEntity(car, account("emil"), CarRole.DRIVER, peter);
			ended.setValidUntil(OffsetDateTime.now().minusDays(1));
			carAccessRepository.persist(ended);
		});
	}

	@AfterEach
	void cleanup()
	{
		QuarkusTransaction.requiringNew().run(() -> accountRepository.deleteAll());
	}

	private AccountEntity account(String name)
	{
		AccountEntity account = new AccountEntity();
		account.setUserId(name);
		account.setMaxCars(1);
		accountRepository.persist(account);
		return account;
	}

	private byte[] download()
	{
		return given().get("/api/user/me/backup").then().statusCode(200).extract().asByteArray();
	}

	private BackupFile backupFile(byte[] zip) throws IOException
	{
		try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip)))
		{
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null)
			{
				if (entry.getName().equals(BackupFile.ENTRY))
				{
					return mapper.readValue(in.readAllBytes(), BackupFile.class);
				}
			}
		}
		throw new IllegalStateException("No " + BackupFile.ENTRY);
	}

	@Test
	void shouldBackUpTheOpenAndAcceptedAccesses() throws IOException
	{
		List<BackupFile.Access> accesses = backupFile(download()).cars().getFirst().accesses();

		assertEquals(List.of("paul", "newbie@example.com"),
				accesses.stream().map(BackupFile.Access::account).toList());
		assertEquals(CarRole.VIEWER, accesses.getFirst().role());
		assertTrue(accesses.get(1).validUntil() != null);
	}

	@Test
	void shouldRestoreTheAccessesAsInvitations()
	{
		byte[] backup = download();
		given().delete("/api/car/my/{id}", carId).then().statusCode(204);

		given().contentType(ContentType.MULTIPART)
				.multiPart("file", "backup.zip", backup, "application/zip")
				.post("/api/user/me/backup").then()
				.statusCode(200)
				.body("cars", is(1))
				.body("invitations", is(2));

		long restored = given().get("/api/car/my").then().extract().jsonPath().getLong("[0].id");
		given().get("/api/access/{carId}", restored).then().statusCode(200)
				.body("size()", is(2))
				.body("account", contains("paul", null))
				.body("invitee", contains(null, "newbie@example.com"))
				.body("role", contains("VIEWER", "DRIVER"))
				.body("state", everyItem(is("PENDING")))
				.body("[1].validUntil", notNullValue());
		// paul accepts again first
		assertTrue(carAccessRepository.rolesOf("paul").isEmpty());
	}

	@Test
	@TestSecurity(user = "paul", roles = "user")
	void shouldNeverPutASharedCarIntoTheBackupOfAnother() throws IOException
	{
		assertEquals(List.of(), backupFile(download()).cars());
	}
}
