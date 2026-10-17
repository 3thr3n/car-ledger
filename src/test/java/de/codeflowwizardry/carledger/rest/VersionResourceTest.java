package de.codeflowwizardry.carledger.rest;

import static io.restassured.RestAssured.given;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class VersionResourceTest
{
	/** the revision of the build: "dev" locally, e.g. 0.5.0-dev.540 in the CI */
	@ConfigProperty(name = "quarkus.application.version")
	String version;

	@Test
	@TestSecurity(user = "peter", roles = "user")
	void shouldTellTheVersionOfTheBuild()
	{
		given()
				.when()
				.get("/api/version")
				.then()
				.statusCode(200)
				.body("version", Matchers.is(version))
				.body("version", Matchers.not(Matchers.emptyOrNullString()));
	}
}
