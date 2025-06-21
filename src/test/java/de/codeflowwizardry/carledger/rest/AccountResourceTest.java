package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class AccountResourceTest extends AbstractResourceTest
{
	@Inject
	AccountRepository accountRepository;

	@Test
	void shouldCreateAccount()
	{
		// given
		assertEquals(0, accountRepository.count());

		// when
		given()
				.cookie(cookie)
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
				.cookie(cookie)
				.when()
				.get("/api/user/me")
				.then()
				.statusCode(200)
				.body("maxCars", Matchers.is(1));
		given()
				.cookie(cookie)
				.when()
				.get("/api/user/me")
				.then()
				.statusCode(200)
				.body("maxCars", Matchers.is(1));

		// then
		assertEquals(1, accountRepository.count());
	}

	@BeforeEach
	@AfterEach
	@Transactional
	void reset()
	{
		accountRepository.deleteAll();
	}

}
