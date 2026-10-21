package de.codeflowwizardry.carledger.rest.car.tracker;

import java.util.List;

import io.quarkus.security.identity.SecurityIdentity;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.tracker.DeviceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The GPS trackers of a car, only for its owner
 */
@Path("tracker/{carId}")
public class TrackerResource extends AbstractResource
{
	private final DeviceService deviceService;

	@Inject
	public TrackerResource(SecurityIdentity identity, AccountRepository accountRepository,
			DeviceService deviceService)
	{
		super(identity, accountRepository);
		this.deviceService = deviceService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getTrackers", description = "GPS trackers of a car, the newest first. Empty for a car that isn't yours.")
	public List<Device> getTrackers(@PathParam("carId") long carId)
	{
		return deviceService.list(carId, context.getName());
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addTracker", description = "Creates a GPS tracker. The answer contains its token, it can't be shown again.")
	@APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = DeviceCreated.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public Response addTracker(@PathParam("carId") long carId, DeviceInput input)
	{
		return Response.status(Response.Status.CREATED).entity(deviceService.create(carId, input, context.getName()))
				.build();
	}

	@POST
	@Path("{id}/revoke")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "revokeTracker", description = "The token of the tracker stops working, its points stay.")
	@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Device.class)))
	@APIResponse(responseCode = "404", description = "Tracker not found for the car of your user.")
	public Device revokeTracker(@PathParam("carId") long carId, @PathParam("id") long id)
	{
		return deviceService.revoke(carId, id, context.getName());
	}
}
