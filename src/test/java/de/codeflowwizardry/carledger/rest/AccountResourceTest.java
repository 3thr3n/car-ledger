package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

@QuarkusTest
class AccountResourceTest
{
	@Inject
	AccountRepository accountRepository;

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
				QuarkusSecurityIdentity.builder().setPrincipal(principal).build(), repository).checkUser("peter");

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

}
