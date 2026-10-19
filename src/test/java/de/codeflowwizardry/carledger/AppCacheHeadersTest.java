package de.codeflowwizardry.carledger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AppCacheHeadersTest
{
	@Test
	void shouldLetTheBrowserAskAgainForTheFilesWithoutAHash()
	{
		// with or without the build of the frontend (-Dquarkus.quinoa=false)
		for (String path : new String[] {
				"/", "/index.html", "/sw.js", "/manifest.json", "/car/1/trips"
		})
		{
			given().when().get(path).then().header("Cache-Control", is(AppCacheHeaders.NO_CACHE));
		}
	}

	@Test
	void shouldKeepTheCacheOfTheFilesWithAHashAndOfTheApi()
	{
		given().when().get("/assets/index-abc123.js").then()
				.header("Cache-Control", Matchers.anyOf(nullValue(), not(is(AppCacheHeaders.NO_CACHE))));
		given().when().get("/q/health/live").then()
				.header("Cache-Control", Matchers.anyOf(nullValue(), not(is(AppCacheHeaders.NO_CACHE))));
	}

	@Test
	void shouldKnowTheFilesOfTheFrontend()
	{
		assertTrue(AppCacheHeaders.isAppShell("/sw.js"));
		assertTrue(AppCacheHeaders.isAppShell("/dashboard"));
		assertFalse(AppCacheHeaders.isAppShell("/assets/index-abc123.js"));
		assertFalse(AppCacheHeaders.isAppShell("/api/user/me"));
		assertFalse(AppCacheHeaders.isAppShell("/q/health/live"));
	}
}
