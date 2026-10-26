package de.codeflowwizardry.carledger.rest.car.access;

import java.util.List;

import io.quarkus.security.identity.SecurityIdentity;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.CarAccessService;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The accounts with access to a car and the invitations, only for its owner
 */
@Path("access/{carId}")
public class CarAccessResource extends AbstractResource
{
	private final CarAccessService carAccessService;

	@Inject
	public CarAccessResource(SecurityIdentity identity, AccountRepository accountRepository,
			CarAccessService carAccessService)
	{
		super(identity, accountRepository);
		this.carAccessService = carAccessService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getCarAccess", description = "The accesses and invitations of a car, the oldest first.")
	@APIResponse(responseCode = "200", content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = CarAccessEntry.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public List<CarAccessEntry> getCarAccess(@PathParam("carId") long carId)
	{
		return carAccessService.list(carId, context.getName());
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "inviteToCar", description = "Invites an account by its user name or e-mail address. One that doesn't exist yet gets the invitation with its first login, a declined one is asked again.")
	@APIResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CarAccessEntry.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	@APIResponse(responseCode = "409", description = "The account has the access or the invitation already.")
	public Response inviteToCar(@PathParam("carId") long carId, CarInvitationInput input)
	{
		return Response.status(Response.Status.CREATED).entity(carAccessService.invite(carId, input,
				context.getName())).build();
	}

	@POST
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "changeCarAccess", description = "Changes the role or the period of an access or an invitation.")
	@APIResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CarAccessEntry.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Access not found for the car of your user.")
	public CarAccessEntry changeCarAccess(@PathParam("carId") long carId, @PathParam("id") long id,
			CarAccessInput input)
	{
		return carAccessService.change(carId, id, input, context.getName());
	}

	@DELETE
	@Path("{id}")
	@Operation(operationId = "revokeCarAccess", description = "Ends the access early or takes back the invitation. The entries of the account stay with the car.")
	@APIResponse(responseCode = "204")
	@APIResponse(responseCode = "404", description = "Access not found for the car of your user.")
	public void revokeCarAccess(@PathParam("carId") long carId, @PathParam("id") long id)
	{
		carAccessService.revoke(carId, id, context.getName());
	}
}
