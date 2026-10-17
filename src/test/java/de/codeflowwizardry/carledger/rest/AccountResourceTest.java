package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.hamcrest.Matchers;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import java.time.OffsetDateTime;

import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

@QuarkusTest
class AccountResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldCreateAccount()
	{
		// given
		assertEquals(0, accountRepository.count());

		// when
		given()
				.when()
				.get("/api/user/me")
				.then()
				.statusCode(200)
				.body("maxCars", Matchers.is(1));

		// then
		assertEquals(1, accountRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldCreatAndSelectSameAccount()
	{
		// given
		assertEquals(0, accountRepository.count());

		// when
		given()
				.when()
				.get("/api/user/me")
				.then()
				.statusCode(200)
				.body("maxCars", Matchers.is(1));

		given()
				.when()
				.get("/api/user/me")
				.then()
				.statusCode(200)
				.body("maxCars", Matchers.is(1));

		// then
		assertEquals(1, accountRepository.count());
	}

	@Test
	void shouldFailCreatingSameAccountTwiceInsideCreateAccount()
	{
		// given
		accountRepository.createAccount("peter", 1);

		// when / then: the duplicate surfaces immediately, where checkUser can catch it
		assertThrows(ConstraintViolationException.class, () -> accountRepository.createAccount("peter", 1));
		assertEquals(1, accountRepository.count());
	}

	@Test
	void shouldReturnExistingAccountWhenConcurrentFirstLoginCreatedIt()
	{
		// given: lookup misses, but another request inserts the account before we do
		AccountEntity existing = new AccountEntity();
		existing.setUserId("peter");

		AccountRepository repository = mock(AccountRepository.class);
		when(repository.findByIdentifier("peter"))
				.thenThrow(new BadRequestException("User cannot be found! peter"))
				.thenReturn(existing);
		when(repository.createAccount("peter", 1))
				.thenThrow(new ConstraintViolationException("duplicate", null, "account_user_id_key"));

		Principal principal = () -> "peter";

		// when
		AccountEntity accountEntity = new AccountResource(
				QuarkusSecurityIdentity.builder().setPrincipal(principal).build(), repository, null)
				.checkUser("peter");

		// then
		assertSame(existing, accountEntity);
	}

	@Test
	void shouldBeRedirectToLoginUrl()
	{
		String response = given()
				.when()
				.get("/api/car/my")
				.then()
				.statusCode(200)
				.extract()
				.response()
				.getBody()
				.asString();
		assertTrue(response.contains("<html"));
	}

	@BeforeEach
	@AfterEach
	@Transactional
	void reset()
	{
		accountRepository.deleteAll();
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldReturnDefaultSettingsAndNoAdmin()
	{
		given()
				.when()
				.get("/api/user/me")
				.then()
				.statusCode(200)
				.body("admin", Matchers.is(false))
				.body("vatCountry", Matchers.is("DE"))
				.body("currency", Matchers.is("EUR"))
				.body("language", Matchers.nullValue())
				.body("unitSystem", Matchers.is("METRIC"));

		assertNotNull(lastSeen("peter"));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user", "admin"
	})
	void shouldReturnAdminFlag()
	{
		given()
				.when()
				.get("/api/user/me")
				.then()
				.statusCode(200)
				.body("admin", Matchers.is(true));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldUpdateSettings()
	{
		given().when().get("/api/user/me").then().statusCode(200);

		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "vatCountry": "AT", "currency": "CHF", "language": "DE" }
						""")
				.when()
				.put("/api/user/me/settings")
				.then()
				.statusCode(200)
				.body("vatCountry", Matchers.is("AT"))
				.body("currency", Matchers.is("CHF"))
				.body("language", Matchers.is("DE"));

		given()
				.when()
				.get("/api/user/me")
				.then()
				.body("currency", Matchers.is("CHF"));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldKeepTheUnitSystemWithoutOne()
	{
		given().when().get("/api/user/me").then().statusCode(200);

		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "vatCountry": "US", "currency": "USD", "language": "EN", "unitSystem": "US" }
						""")
				.when()
				.put("/api/user/me/settings")
				.then()
				.statusCode(200)
				.body("unitSystem", Matchers.is("US"));

		// a client of an older version does not know the units
		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "vatCountry": "US", "currency": "USD", "language": "EN" }
						""")
				.when()
				.put("/api/user/me/settings")
				.then()
				.statusCode(200)
				.body("unitSystem", Matchers.is("US"));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldRejectInvalidSettings()
	{
		given().when().get("/api/user/me").then().statusCode(200);

		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "vatCountry": "XX", "currency": "EURO", "language": "FR" }
						""")
				.when()
				.put("/api/user/me/settings")
				.then()
				.statusCode(400)
				.body("size()", Matchers.is(3));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldDeleteOwnAccountWithItsCars()
	{
		given().when().get("/api/user/me").then().statusCode(200);
		createCar("peter");
		createOtherAccount("paul");

		given()
				.when()
				.delete("/api/user/me")
				.then()
				.statusCode(204);

		assertEquals(1, accountRepository.count());
		assertEquals(0, carRepository.count());
		assertEquals("paul", accountRepository.findAll().firstResult().getUserId());
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldAcceptDeletingMissingAccount()
	{
		given()
				.when()
				.delete("/api/user/me")
				.then()
				.statusCode(204);

		assertEquals(0, accountRepository.count());
	}

	@Test
	@TestSecurity(user = "peter", roles = "admin")
	@OidcSecurity(claims = @Claim(key = "email", value = "peter@example.com"))
	void shouldRememberTheAddressTheRoleAndTheLanguageOfTheBrowser()
	{
		given()
				.header("Accept-Language", "fr-FR, de;q=0.8, en;q=0.5")
				.when()
				.get("/api/user/me")
				.then()
				.statusCode(200);

		AccountEntity account = account("peter");
		assertEquals("peter@example.com", account.getEmail());
		assertTrue(account.isAdmin());
		assertEquals("DE", account.getBrowserLanguage());
		assertEquals("DE", account.getMailLanguage());
	}

	@Test
	@TestSecurity(user = "peter", roles = "user")
	@OidcSecurity(claims = @Claim(key = "email", value = "peter@example.com"))
	void shouldTurnTheMailsOff()
	{
		given().get("/api/user/me").then().statusCode(200);

		given()
				.when()
				.get("/api/user/me/notifications")
				.then()
				.statusCode(200)
				.body("available", Matchers.is(true))
				.body("email", Matchers.is("peter@example.com"))
				.body("newRequests", Matchers.is(true))
				.body("requestDecided", Matchers.is(true));

		given()
				.contentType(ContentType.JSON)
				.body("""
						{ "requestDecided": false }
						""")
				.when()
				.put("/api/user/me/notifications")
				.then()
				.statusCode(200)
				.body("newRequests", Matchers.is(true))
				.body("requestDecided", Matchers.is(false));

		AccountEntity account = account("peter");
		assertFalse(account.isAdmin());
		assertFalse(account.isNotifyRequestDecided());
		assertEquals("EN", account.getMailLanguage());
	}

	@Transactional
	AccountEntity account(String user)
	{
		return accountRepository.findByIdentifier(user);
	}

	@Transactional
	void createCar(String user)
	{
		CarEntity car = new CarEntity();
		car.setUser(accountRepository.findByIdentifier(user));
		car.setName("Peters car");
		carRepository.persist(car);
	}

	void createOtherAccount(String user)
	{
		accountRepository.createAccount(user, 1);
	}

	@Transactional
	OffsetDateTime lastSeen(String user)
	{
		return accountRepository.findByIdentifier(user).getLastSeen();
	}
}
