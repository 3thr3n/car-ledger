package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.KeycloakTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static io.restassured.RestAssured.given;

@QuarkusTestResource(KeycloakTestResource.class)
public class AbstractResourceTest {
    public final static String BOB_LOGIN = """
			{
				"username": "bob",
				"password": "bob"
			}
			""";

    public final static String ALICE_LOGIN = """
			{
				"username": "alice",
				"password": "alice"
			}
			""";

    protected Cookie bobCookie;
    protected Cookie aliceCookie;

    @BeforeEach
    public void setUp() {
        bobCookie = given()
                .contentType(ContentType.JSON)
                .body(BOB_LOGIN)
                .when()
                .post("api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .detailedCookie("SESSION_ID");

        aliceCookie = given()
                .contentType(ContentType.JSON)
                .body(ALICE_LOGIN)
                .when()
                .post("api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .detailedCookie("SESSION_ID");
    }

    @AfterEach
    public void tearDown() {
        if (bobCookie != null) {
            given()
                    .cookie(bobCookie)
                    .when()
                    .post("api/auth/logout")
                    .then()
                    .statusCode(200);
        }
        if (aliceCookie != null) {
            given()
                    .cookie(aliceCookie)
                    .when()
                    .post("api/auth/logout")
                    .then()
                    .statusCode(200);
        }
    }
}
