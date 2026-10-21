package de.codeflowwizardry.carledger.map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
@TestProfile(ContentSecurityPolicyTest.WithMap.class)
class ContentSecurityPolicyTest
{
	public static class WithMap implements QuarkusTestProfile
	{
		@Override
		public Map<String, String> getConfigOverrides()
		{
			return Map.of("carledger.map.style-url", "https://maps.example.com/styles/liberty.json?key=abc",
					"carledger.map.sources", "https://tiles.example.com/, https://glyphs.example.com");
		}
	}

	private static MapConfig config(String styleUrl, List<String> sources)
	{
		return new MapConfig() {
			@Override
			public Optional<String> styleUrl()
			{
				return Optional.ofNullable(styleUrl);
			}

			@Override
			public Optional<List<String>> sources()
			{
				return Optional.ofNullable(sources);
			}
		};
	}

	@Test
	void shouldAllowTheOriginsOfTheMapOnThePagesOfTheApp()
	{
		String origins = "https://maps.example.com https://tiles.example.com https://glyphs.example.com";
		for (String path : new String[] {
				"/", "/car/1/track"
		})
		{
			given().when().get(path).then()
					.header(ContentSecurityPolicy.HEADER, containsString("img-src 'self' data: blob: " + origins + ";"))
					.header(ContentSecurityPolicy.HEADER, containsString("connect-src 'self' " + origins + ";"))
					.header(ContentSecurityPolicy.HEADER, containsString("frame-ancestors 'none'"))
					.header("X-Frame-Options", "DENY");
		}
	}

	@Test
	void shouldNotSetThePolicyOfTheAppOnTheApi()
	{
		// the API keeps its own headers
		given().when().get("/api/version").then()
				.header(ContentSecurityPolicy.HEADER, anyOf(nullValue(), not(containsString("img-src"))));
	}

	@Test
	@TestSecurity(user = "peter", roles = {
			"user"
	})
	void shouldReturnTheStyle()
	{
		given().when().get("/api/map").then()
				.statusCode(200)
				.body("styleUrl", is("https://maps.example.com/styles/liberty.json?key=abc"));
	}

	@Test
	void shouldLeaveOutInvalidOrigins()
	{
		Set<String> origins = ContentSecurityPolicy.mapOrigins(config("http://localhost:8081/style.json",
				List.of("https://*.tiles.example.com", "https://a.example.com; script-src *", "data:",
						"tiles.example.com")));

		assertEquals(Set.of("http://localhost:8081", "https://*.tiles.example.com"), origins);
	}

	@Test
	void shouldKeepThePolicyWithoutAMap()
	{
		String policy = ContentSecurityPolicy.policy(ContentSecurityPolicy.mapOrigins(config(null, null)));

		assertEquals("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
				+ "img-src 'self' data: blob:; font-src 'self'; connect-src 'self'; worker-src 'self'; "
				+ "manifest-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; "
				+ "frame-ancestors 'none'", policy);
		given().when().get("/").then().header(ContentSecurityPolicy.HEADER, not(nullValue()));
	}
}
