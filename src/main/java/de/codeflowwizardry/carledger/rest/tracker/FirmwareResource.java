package de.codeflowwizardry.carledger.rest.tracker;

import java.io.File;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.tracker.FirmwareManifest;
import de.codeflowwizardry.carledger.tracker.TrackerFirmware;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The firmware of the GPS trackers, the browser flashes it over USB. It isn't
 * public: only for logged-in users, only the files of the manifest.
 */
@Path("tracker/firmware")
public class FirmwareResource
{
	private final TrackerFirmware firmware;

	@Inject
	public FirmwareResource(TrackerFirmware firmware)
	{
		this.firmware = firmware;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getFirmwareSettings", description = "Version of the firmware, without one flashing isn't set up and hidden.")
	public FirmwareSettings getFirmwareSettings()
	{
		return new FirmwareSettings(firmware.manifest().map(FirmwareManifest::version).orElse(null));
	}

	@GET
	@Path("manifest.json")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getFirmwareManifest", description = "The files of the firmware and where they are written.")
	@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = FirmwareManifest.class)))
	@APIResponse(responseCode = "404", description = "Flashing isn't set up.")
	public FirmwareManifest getFirmwareManifest()
	{
		return firmware.manifest().orElseThrow(() -> new NotFoundException("Flashing isn't set up."));
	}

	@GET
	@Path("{file}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(operationId = "getFirmwareFile", description = "A file of the firmware, one of the parts of the manifest.")
	@APIResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(type = SchemaType.STRING, format = "binary")))
	@APIResponse(responseCode = "404", description = "Not a file of the manifest, or flashing isn't set up.")
	public Response getFirmwareFile(@PathParam("file") String file)
	{
		File content = firmware.file(file).orElseThrow(() -> new NotFoundException("No file of the firmware."))
				.toFile();
		return Response.ok(content).header("Cache-Control", "no-store").build();
	}
}
