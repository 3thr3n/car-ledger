package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.KeycloakTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static de.codeflowwizardry.carledger.rest.AbstractResourceTest.BOB_LOGIN;
import static io.restassured.RestAssured.given;

@QuarkusTest
@TestHTTPEndpoint(AuthResource.class)
@QuarkusTestResource(KeycloakTestResource.class)
class AuthResourceTest
{
	@Test
	void shouldLogin()
	{
		given()
				.contentType(ContentType.JSON)
				.body(BOB_LOGIN)
				.when()
				.post("login")
				.then()
				.statusCode(200)
				.cookie("SESSION_ID", Matchers.notNullValue());
	}

	@Test
	void shouldFailWithoutCookie()
	{
		given()
				.when()
				.get("log")
				.then()
				.statusCode(401);
	}

	@Test
	void shouldLoginAndBeAuthenticated()
	{
		Cookie loginCookie = given()
				.contentType(ContentType.JSON)
				.body(BOB_LOGIN)
				.when()
				.post("login")
				.then()
				.statusCode(200)
				.extract().detailedCookie("SESSION_ID");

		given()
				.cookie(loginCookie)
				.get("log")
				.then()
				.statusCode(200);
	}

	@Test
	void shouldLoginAndLogout()
	{
		Cookie loginCookie = given()
				.contentType(ContentType.JSON)
				.body(BOB_LOGIN)
				.when()
				.post("login")
				.then()
				.statusCode(200)
				.extract().detailedCookie("SESSION_ID");

		given()
				.cookie(loginCookie)
				.post("logout")
				.then()
				.statusCode(200);

		given()
				.cookie(loginCookie)
				.get("log")
				.then()
				.statusCode(401);
	}
}
