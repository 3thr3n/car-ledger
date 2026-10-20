package de.codeflowwizardry.carledger.rest.car.trip;

import io.quarkus.security.identity.SecurityIdentity;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.OdometerResetService;
import de.codeflowwizardry.carledger.OdometerResetService.Added;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Odometer resets of the trip log of a car: a replaced instrument cluster or a
 * reset odometer. Added by the owner, not edited.
 */
@Path("trip/{carId}/resets")
public class OdometerResetResource extends AbstractResource
{
	private final OdometerResetService resetService;

	@Inject
	public OdometerResetResource(SecurityIdentity identity, AccountRepository accountRepository,
			OdometerResetService resetService)
	{
		super(identity, accountRepository);
		this.resetService = resetService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getOdometerResets", description = "Odometer resets of a car, the latest first, also the deleted ones")
	@APIResponse(responseCode = "200", description = "Car found.")
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public List<OdometerReset> getOdometerResets(@PathParam("carId") long carId)
	{
		return resetService.list(carId, context.getName());
	}

	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addOdometerReset", description = "Adds an odometer reset with the id created by the client, only the owner of the car. From its time on the odometer counts from the new reading. Sent again with the same values, it answers with the reset.")
	@APIResponse(responseCode = "201", description = "Reset added.", content = @Content(schema = @Schema(implementation = OdometerReset.class)))
	@APIResponse(responseCode = "200", description = "Reset was added before with the same values.", content = @Content(schema = @Schema(implementation = OdometerReset.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not one you own.")
	@APIResponse(responseCode = "409", description = "The reset doesn't fit the trips of the car or the id is in use.", content = @Content(schema = @Schema(implementation = TripConflict.class)))
	public Response addOdometerReset(@PathParam("carId") long carId, @PathParam("id") UUID id,
			OdometerResetInput input)
	{
		Added added = resetService.add(carId, id, input, context.getName());
		if (!added.created())
		{
			return Response.ok(added.reset()).build();
		}
		return Response.created(URI.create("trip/" + carId + "/resets/" + id)).entity(added.reset()).build();
	}

	@POST
	@Path("{id}/delete")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "deleteOdometerReset", description = "Deletes an odometer reset with a reason, only the owner of the car. It stays listed as deleted and doesn't count anymore. Sent again, it answers with the deleted reset.")
	@APIResponse(responseCode = "200", description = "Reset deleted.", content = @Content(schema = @Schema(implementation = OdometerReset.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Reset not found for a car you own.")
	public OdometerReset deleteOdometerReset(@PathParam("carId") long carId, @PathParam("id") UUID id,
			TripDeleteInput input)
	{
		return resetService.delete(carId, id, input, context.getName());
	}
}
