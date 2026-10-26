package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.CarAccessService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.CarRole;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccessRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.access.CarInvitationInput;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;

/**
 * peter invites paul to his car: paul sees it only once he accepted, peter
 * changes and revokes the access
 */
@QuarkusTest
@TestSecurity(user = "peter", roles = "user")
class CarInvitationTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	CarAccessRepository carAccessRepository;

	@Inject
	CarAccessService carAccessService;

	@Inject
	MockMailbox mailbox;

	long carId;

	@BeforeEach
	void setup()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			AccountEntity peter = account("peter", "peter@example.com");
			account("paul", "paul@example.com");

			CarEntity car = new CarEntity();
			car.setUser(peter);
			car.setName("Golf");
			car.setManufactureYear(2020);
			car.setPurchaseDate(LocalDate.of(2020, 5, 1));
			carRepository.persist(car);
			carId = car.getId();
		});
		mailbox.clear();
	}

	@AfterEach
	void cleanup()
	{
		QuarkusTransaction.requiringNew().run(() -> accountRepository.deleteAll());
	}

	private AccountEntity account(String name, String email)
	{
		AccountEntity account = new AccountEntity();
		account.setUserId(name);
		account.setMaxCars(1);
		account.setEmail(email);
		account.setEmailVerified(email != null);
		accountRepository.persist(account);
		return account;
	}

	private ValidatableResponse invite(String account, String role)
	{
		Map<String, Object> body = new HashMap<>();
		body.put("account", account);
		body.put("role", role);
		return given().contentType(ContentType.JSON).body(body).put("/api/access/{carId}", carId).then();
	}

	/**
	 * @return the id of the invitation of paul as a driver
	 */
	private long invitePaul()
	{
		return invite("Paul", "DRIVER").statusCode(201).extract().jsonPath().getLong("id");
	}

	/**
	 * @return the mails to the address, once their number reached the expected
	 *         one or after a second
	 */
	private List<Mail> mailsTo(String address, int expected) throws InterruptedException
	{
		long until = System.nanoTime() + Duration.ofSeconds(1).toNanos();
		while (mailbox.getMailsSentTo(address).size() < expected && System.nanoTime() < until)
		{
			Thread.sleep(20);
		}
		return mailbox.getMailsSentTo(address);
	}

	private void paulAnswers(long id, boolean accept)
	{
		carAccessService.answer(id, accept, "paul");
	}

	private Map<Long, ?> rolesOfPaul()
	{
		return carAccessRepository.rolesOf("paul");
	}

	@Test
	void shouldInviteAnAccountThatGetsTheCarOnlyAfterAccepting() throws Exception
	{
		long id = invitePaul();

		given().get("/api/access/{carId}", carId).then().statusCode(200)
				.body("size()", is(1))
				.body("[0].account", is("paul"))
				.body("[0].invitee", nullValue())
				.body("[0].role", is("DRIVER"))
				.body("[0].state", is("PENDING"));
		Mail invitation = mailsTo("paul@example.com", 1).getFirst();
		assertEquals("peter shares the car \"Golf\" with you", invitation.getSubject());
		assertTrue(invitation.getText().contains("as a driver"), invitation.getText());
		assertTrue(rolesOfPaul().isEmpty());

		paulAnswers(id, true);

		given().get("/api/access/{carId}", carId).then().body("[0].state", is("ACCEPTED"));
		assertEquals(Map.of(carId, CarRole.DRIVER), rolesOfPaul());
		Mail answer = mailsTo("peter@example.com", 1).getFirst();
		assertEquals("paul accepted the access to \"Golf\"", answer.getSubject());
	}

	@Test
	@TestSecurity(user = "paul", roles = "user")
	void shouldListAndAcceptTheInvitationsOfTheUser()
	{
		long id = invitedAsPeter("Paul");

		given().get("/api/car/my").then().statusCode(200).body("size()", is(0));
		given().get("/api/invitations").then().statusCode(200)
				.body("size()", is(1))
				.body("[0].id", is((int) id))
				.body("[0].carId", is((int) carId))
				.body("[0].car", is("Golf"))
				.body("[0].owner", is("peter"))
				.body("[0].role", is("DRIVER"));

		given().post("/api/invitations/{id}/accept", id).then().statusCode(204);

		given().get("/api/invitations").then().statusCode(200).body("size()", is(0));
		given().get("/api/car/my").then().statusCode(200)
				.body("size()", is(1))
				.body("[0].role", is("DRIVER"));
		// answered once
		given().post("/api/invitations/{id}/decline", id).then().statusCode(404);
		// not the owner
		given().get("/api/access/{carId}", carId).then().statusCode(404);
		given().contentType(ContentType.JSON).body(Map.of("account", "mary", "role", "VIEWER"))
				.put("/api/access/{carId}", carId).then().statusCode(404);
		given().delete("/api/access/{carId}/{id}", carId, id).then().statusCode(404);
	}

	@Test
	@TestSecurity(user = "mary", roles = "user")
	void shouldNotLetAnotherAccountAnswer()
	{
		QuarkusTransaction.requiringNew().run(() -> account("mary", null));
		long id = invitedAsPeter("paul");

		given().get("/api/invitations").then().body("size()", is(0));
		given().post("/api/invitations/{id}/accept", id).then().statusCode(404);
	}

	@Test
	@TestSecurity(user = "newbie", roles = "user")
	void shouldGiveTheInvitationToANewAccountWithItsFirstLogin()
	{
		long id = invitedAsPeter("Newbie");
		assertEquals("newbie", carAccessRepository.findById(id).getInvitee());

		given().get("/api/user/me").then().statusCode(200);

		given().get("/api/invitations").then().body("size()", is(1)).body("[0].car", is("Golf"));
		assertEquals("newbie", QuarkusTransaction.requiringNew()
				.call(() -> carAccessService.list(carId, "peter").getFirst().account()));
	}

	/**
	 * @return the id of the invitation as a driver, made by peter
	 */
	private long invitedAsPeter(String account)
	{
		return carAccessService.invite(carId, new CarInvitationInput(account, CarRole.DRIVER, null, null), "peter")
				.id();
	}

	@Test
	void shouldAskAgainAfterADecline()
	{
		long id = invitePaul();
		paulAnswers(id, false);

		given().get("/api/access/{carId}", carId).then().body("[0].state", is("DECLINED"));
		assertTrue(rolesOfPaul().isEmpty());

		invite("paul", "VIEWER").statusCode(201)
				.body("id", is((int) id))
				.body("state", is("PENDING"))
				.body("role", is("VIEWER"));
	}

	@Test
	void shouldEndTheAccessByItselfAndAskAgainAfterIt()
	{
		long id = invitePaul();
		paulAnswers(id, true);
		assertEquals(Map.of(carId, CarRole.DRIVER), rolesOfPaul());

		// the end passed, without a job
		QuarkusTransaction.requiringNew().run(
				() -> carAccessRepository.findById(id).setValidUntil(OffsetDateTime.now().minusMinutes(1)));

		assertTrue(rolesOfPaul().isEmpty());
		// the owner still sees it, as it was
		given().get("/api/access/{carId}", carId).then()
				.body("[0].state", is("ACCEPTED"))
				.body("[0].validUntil", notNullValue());

		invite("paul", "DRIVER").statusCode(201)
				.body("id", is((int) id))
				.body("state", is("PENDING"))
				.body("validUntil", nullValue());
	}

	@Test
	void shouldRejectASecondInvitationAndTheOwner()
	{
		invitePaul();

		invite("paul", "EDITOR").statusCode(409);
		invite("paul@example.com", "EDITOR").statusCode(409);
		invite("peter", "EDITOR").statusCode(400).body("", hasItem("You own the car!"));
		invite("paul", "OWNER").statusCode(400);
		invite(" ", "EDITOR").statusCode(400);
	}

	@Test
	void shouldCheckThePeriod()
	{
		OffsetDateTime now = OffsetDateTime.now();
		given().contentType(ContentType.JSON)
				.body(Map.of("account", "paul", "role", "DRIVER", "validFrom", now.plusDays(2).toString(),
						"validUntil", now.plusDays(1).toString()))
				.put("/api/access/{carId}", carId).then().statusCode(400)
				.body("", hasItem("The end must be after the start!"));
		given().contentType(ContentType.JSON)
				.body(Map.of("account", "paul", "role", "DRIVER", "validUntil", now.minusDays(1).toString()))
				.put("/api/access/{carId}", carId).then().statusCode(400)
				.body("", hasItem("The end must be in the future!"));
	}

	@Test
	void shouldChangeAndRevokeTheAccess()
	{
		long id = invitePaul();
		paulAnswers(id, true);

		OffsetDateTime until = OffsetDateTime.now().plusDays(14);
		given().contentType(ContentType.JSON).body(Map.of("role", "EDITOR", "validUntil", until.toString()))
				.post("/api/access/{carId}/{id}", carId, id).then().statusCode(200)
				.body("role", is("EDITOR"))
				.body("state", is("ACCEPTED"))
				.body("validUntil", notNullValue());
		assertEquals(Map.of(carId, CarRole.EDITOR), rolesOfPaul());

		given().delete("/api/access/{carId}/{id}", carId, id).then().statusCode(204);

		given().get("/api/access/{carId}", carId).then().body("size()", is(0));
		assertTrue(rolesOfPaul().isEmpty());
	}

	@Test
	void shouldFindAnAddressOnlyOnceTheIdentityProviderConfirmedIt()
	{
		QuarkusTransaction.requiringNew().run(() -> account("mary", null).setEmail("mary@example.com"));

		// not confirmed: kept for an account with the address
		invite("mary@example.com", "VIEWER").statusCode(201)
				.body("account", nullValue())
				.body("invitee", is("mary@example.com"));

		QuarkusTransaction.requiringNew().run(() -> {
			AccountEntity mary = accountRepository.findByIdentifier("mary");
			mary.setEmailVerified(true);
			carAccessService.claim(mary, mary.getEmail());
		});
		given().get("/api/access/{carId}", carId).then().body("[0].account", is("mary"));
	}

	@Test
	void shouldDropAnInvitationByAddressForACarTheAccountHasAlready()
	{
		long id = invitePaul();
		invite("paul@elsewhere.example", "EDITOR").statusCode(201);

		carAccessService.claim(accountRepository.findByIdentifier("paul"), "paul@elsewhere.example");

		given().get("/api/access/{carId}", carId).then()
				.body("size()", is(1))
				.body("[0].id", is((int) id));
	}
}
