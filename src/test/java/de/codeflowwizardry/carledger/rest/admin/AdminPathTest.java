package de.codeflowwizardry.carledger.rest.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.Path;

/**
 * The administration is only protected by the HTTP permission of /api/admin*
 * (application.properties): every admin resource has to be below it, and the
 * permission has to require the role admin. (@RolesAllowed on the resources
 * does
 * not work with the hybrid OIDC login of the app.)
 */
class AdminPathTest
{
	private static final List<Class<?>> ADMIN_RESOURCES = List.of(AdminResource.class,
			AdminCarRequestResource.class);

	@Test
	void shouldServeTheAdminResourcesBelowTheProtectedPath()
	{
		for (Class<?> resource : ADMIN_RESOURCES)
		{
			String path = resource.getAnnotation(Path.class).value();
			assertTrue(path.equals("admin") || path.startsWith("admin/"),
					resource.getSimpleName() + " is served at /api/" + path + ", not below /api/admin");
		}
	}

	@Test
	void shouldRequireTheRoleAdminForTheProtectedPath() throws IOException
	{
		Properties config = new Properties();
		try (InputStream in = getClass().getResourceAsStream("/application.properties"))
		{
			config.load(in);
		}
		assertEquals("/api/admin*", config.getProperty("quarkus.http.auth.permission.admin.paths"));
		assertEquals("allowAdmin", config.getProperty("quarkus.http.auth.permission.admin.policy"));
		assertEquals("admin", config.getProperty("quarkus.http.auth.policy.allowAdmin.roles-allowed"));
	}

	@Test
	void shouldKnowEveryAdminResource() throws IOException
	{
		// a new resource in this package has to be added to ADMIN_RESOURCES
		long resources;
		try (var classes = java.nio.file.Files
				.list(java.nio.file.Path.of("src/main/java/de/codeflowwizardry/carledger/rest/admin")))
		{
			resources = classes.filter(file -> file.getFileName().toString().endsWith("Resource.java")).count();
		}
		assertEquals(ADMIN_RESOURCES.size(), resources);
	}
}
