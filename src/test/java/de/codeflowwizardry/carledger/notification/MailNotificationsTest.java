package de.codeflowwizardry.carledger.notification;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.request.CarLimitRequests;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * The car requests mail the administrators and the user who asked, after the
 * commit and in their language; the mailer of the tests only collects them
 */
@QuarkusTest
class MailNotificationsTest
{
	@Inject
	MockMailbox mailbox;

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarLimitRequests carLimitRequests;

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();
		// peter asks, alice and bob are administrators with an address, carol turned
		// the mails off and dave has no
		// address, eve is no administrator
		account("peter", "peter@example.com", false, null);
		account("alice", "alice@example.com", true, "DE");
		account("bob", "bob@example.com", true, null);
		account("carol", "carol@example.com", true, null).setNotifyNewRequests(false);
		account("dave", null, true, null);
		account("eve", "eve@example.com", false, null);
		mailbox.clear();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	private AccountEntity account(String name, String email, boolean admin, String language)
	{
		AccountEntity account = new AccountEntity();
		account.setUserId(name);
		account.setMaxCars(1);
		account.setEmail(email);
		account.setAdmin(admin);
		account.setLanguage(language);
		accountRepository.persist(account);
		return account;
	}

	@Transactional
	long peterRequests()
	{
		return carLimitRequests.request(accountRepository.findByIdentifier("peter"), 1, "For the trailer").getId();
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

	@Test
	void shouldMailTheAdministratorsAboutANewRequest() throws Exception
	{
		peterRequests();

		Mail toAlice = mailsTo("alice@example.com", 1).getFirst();
		assertEquals("Neue Fahrzeug-Anfrage von peter", toAlice.getSubject());
		assertTrue(toAlice.getText().contains("peter fragt 1 weiteres Fahrzeug an, das aktuelle Limit ist 1."),
				toAlice.getText());
		assertTrue(toAlice.getText().contains("Grund: „For the trailer“"), toAlice.getText());
		assertTrue(toAlice.getText().contains("https://car-ledger.example.com/admin"), toAlice.getText());

		Mail toBob = mailsTo("bob@example.com", 1).getFirst();
		assertEquals("New car request from peter", toBob.getSubject());
		assertTrue(toBob.getText().contains("peter requests 1 more car, the current limit is 1."), toBob.getText());
		assertTrue(toBob.getText().contains("https://car-ledger.example.com/settings"), toBob.getText());

		// not to carol (turned off), dave (no address), eve (no administrator) and
		// peter (asked)
		assertEquals(2, mailbox.getTotalMessagesSent());
	}

	@Test
	@TestSecurity(user = "alice", roles = "admin")
	void shouldMailTheUserAboutTheApproval() throws Exception
	{
		long id = peterRequests();
		mailsTo("bob@example.com", 1);
		mailbox.clear();

		given()
				.contentType(ContentType.JSON)
				.body("{}")
				.when()
				.post("/api/admin/car-requests/{id}/approve", id)
				.then()
				.statusCode(200);

		Mail toPeter = mailsTo("peter@example.com", 1).getFirst();
		assertEquals("Your car request was approved", toPeter.getSubject());
		assertTrue(toPeter.getText().contains("Your request for 1 more car was approved: you can now have 2 cars."),
				toPeter.getText());
		assertTrue(toPeter.getText().contains("https://car-ledger.example.com/car"), toPeter.getText());
		assertEquals(1, mailbox.getTotalMessagesSent());
	}

	@Test
	@TestSecurity(user = "alice", roles = "admin")
	void shouldMailTheUserAboutTheRejectionInTheLanguageOfTheBrowser() throws Exception
	{
		setBrowserLanguage("peter", "DE");
		long id = peterRequests();
		mailsTo("bob@example.com", 1);
		mailbox.clear();

		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "comment": "Not this year" }
						""")
				.when()
				.post("/api/admin/car-requests/{id}/reject", id)
				.then()
				.statusCode(200);

		Mail toPeter = mailsTo("peter@example.com", 1).getFirst();
		assertEquals("Deine Fahrzeug-Anfrage wurde abgelehnt", toPeter.getSubject());
		assertTrue(toPeter.getText().contains("Kommentar: „Not this year“"), toPeter.getText());
	}

	@Test
	@TestSecurity(user = "alice", roles = "admin")
	void shouldNotMailTheUserWhoTurnedItOff() throws Exception
	{
		turnOffDecisions("peter");
		long id = peterRequests();
		mailsTo("bob@example.com", 1);
		mailbox.clear();

		given()
				.contentType(ContentType.JSON)
				.body("{}")
				.when()
				.post("/api/admin/car-requests/{id}/approve", id)
				.then()
				.statusCode(200);

		Thread.sleep(200);
		assertEquals(0, mailbox.getTotalMessagesSent());
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldNotMailWhenTheRequestFails() throws Exception
	{
		peterRequests();
		mailsTo("bob@example.com", 1);
		mailbox.clear();

		// a second request fails, its transaction sends nothing
		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "cars": 1 }
						""")
				.when()
				.post("/api/user/me/car-requests")
				.then()
				.statusCode(409);

		Thread.sleep(200);
		assertEquals(0, mailbox.getTotalMessagesSent());
	}

	@Transactional
	void setBrowserLanguage(String name, String language)
	{
		accountRepository.findByIdentifier(name).setBrowserLanguage(language);
	}

	@Transactional
	void turnOffDecisions(String name)
	{
		accountRepository.findByIdentifier(name).setNotifyRequestDecided(false);
	}
}
