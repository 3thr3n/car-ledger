package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.hamcrest.Matchers;
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
		// when
		given()
				.cookie(aliceCookie)
				.when()
				.get("/api/user/me")
				.then()
				.statusCode(200)
				.body("maxCars", Matchers.is(1));

		// then
		assertEquals(2, accountRepository.count());
	}

	@Test
	void shouldCreatAndSelectSameAccount()
	{
		// when
		given()
				.cookie(aliceCookie)
				.when()
				.get("/api/user/me")
				.then()
				.statusCode(200)
				.body("maxCars", Matchers.is(1));

		given()
				.cookie(aliceCookie)
				.when()
				.get("/api/user/me")
				.then()
				.statusCode(200)
				.body("maxCars", Matchers.is(1));

		// then
		assertEquals(2, accountRepository.count());
	}
}
