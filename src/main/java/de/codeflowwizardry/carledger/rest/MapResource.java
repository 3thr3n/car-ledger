package de.codeflowwizardry.carledger.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;

import de.codeflowwizardry.carledger.map.MapConfig;
import de.codeflowwizardry.carledger.rest.records.MapSettings;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The map of the GPS tracks, as set up by the operator
 */
@Path("map")
public class MapResource
{
	private final MapConfig config;

	@Inject
	public MapResource(MapConfig config)
	{
		this.config = config;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getMapSettings", description = "Style of the map, without one the map isn't set up and hidden.")
	public MapSettings getMapSettings()
	{
		return new MapSettings(config.styleUrl().orElse(null));
	}
}
