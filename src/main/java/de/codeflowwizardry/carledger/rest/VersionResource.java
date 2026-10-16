package de.codeflowwizardry.carledger.rest;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.rest.records.Version;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The version of the server: the app compares it with its own, an installed
 * app may still run an older one from its cache
 */
@Path("version")
public class VersionResource
{
	private final String version;

	public VersionResource(@ConfigProperty(name = "quarkus.application.version") String version)
	{
		this.version = version;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getVersion", description = "Version of the server, e.g. 0.5.0 or 0.5.0-dev.531")
	@APIResponse(responseCode = "200", description = "Version of the server.")
	public Version getVersion()
	{
		return new Version(version);
	}
}
