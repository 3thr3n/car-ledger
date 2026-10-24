package de.codeflowwizardry.carledger.rest.car.modification;

import io.quarkus.security.identity.SecurityIdentity;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.ModificationService;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("modification/{carId}")
public class ModificationResource extends AbstractResource
{
	private final ModificationService modificationService;

	@Inject
	public ModificationResource(SecurityIdentity identity, AccountRepository accountRepository,
			ModificationService modificationService)
	{
		super(identity, accountRepository);
		this.modificationService = modificationService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getModifications", description = "Modifications of a car, the installed ones first, then the latest installed.")
	public List<Modification> getModifications(@PathParam("carId") long carId,
			@QueryParam("state") @DefaultValue("ALL") ModificationState state)
	{
		return modificationService.list(carId, context.getName(), state);
	}

	@GET
	@Path("bills")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getLinkableBills", description = "The maintenance and other entries of the car a modification can have, the latest first.")
	@APIResponse(responseCode = "200", description = "Bills found.")
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public List<LinkedBill> getLinkableBills(@PathParam("carId") long carId)
	{
		return modificationService.linkableBills(carId, context.getName());
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addModification")
	@APIResponse(responseCode = "202", content = @Content(schema = @Schema(implementation = Modification.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public Response addModification(@PathParam("carId") long carId, ModificationInput input)
	{
		return Response.accepted(modificationService.create(carId, input, context.getName())).build();
	}

	@POST
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateModification", description = "Changes a modification, a removed date moves it into the history.")
	@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Modification.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Modification not found for the car of your user.")
	public Modification updateModification(@PathParam("carId") long carId, @PathParam("id") long id,
			ModificationInput input)
	{
		return modificationService.update(carId, id, input, context.getName());
	}

	@DELETE
	@Path("{id}")
	@Operation(operationId = "deleteModification", description = "Deletes a modification including its history, remove it with a removed date to keep that.")
	@APIResponse(responseCode = "204", description = "Deleted")
	@APIResponse(responseCode = "404", description = "Modification not found for the car of your user.")
	public Response deleteModification(@PathParam("carId") long carId, @PathParam("id") long id)
	{
		modificationService.delete(carId, id, context.getName());
		return Response.noContent().build();
	}
}
