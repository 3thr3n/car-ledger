package de.codeflowwizardry.carledger.rest.tracker;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

/**
 * The fake release of src/test/resources/firmware
 */
@QuarkusTest
class FirmwareResourceTest
{
	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldTellTheVersion()
	{
		given().when().get("/api/tracker/firmware").then()
				.statusCode(200)
				.body("version", is("v1.0.0"));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldServeTheManifest()
	{
		given().when().get("/api/tracker/firmware/manifest.json").then()
				.statusCode(200)
				.body("builds[0].parts.path", contains("bootloader.bin", "partitions.bin", "firmware.bin"))
				.body("config.offset", is(4128768));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldServeOnlyTheFilesOfTheManifest()
	{
		given().when().get("/api/tracker/firmware/firmware.bin").then()
				.statusCode(200)
				.contentType("application/octet-stream")
				.header("Cache-Control", "no-store")
				.body(is("firmware"));

		given().when().get("/api/tracker/firmware/secret.bin").then().statusCode(404);
		given().when().get("/api/tracker/firmware/..%2Fmanifest.json").then().statusCode(404);
	}

	@Test
	/** the login of the OIDC provider instead */
	void shouldNotServeTheFirmwareWithoutLogin()
	{
		given().redirects().follow(false).when().get("/api/tracker/firmware/firmware.bin").then()
				.statusCode(302);
		given().redirects().follow(false).when().get("/api/tracker/firmware/manifest.json").then()
				.statusCode(302);
	}
}
