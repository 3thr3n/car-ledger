package de.codeflowwizardry.carledger.rest.car.access;

import java.util.List;

import io.quarkus.security.identity.SecurityIdentity;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.CarAccessService;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * The invitations of the user to the cars of others
 */
@Path("invitations")
public class CarInvitationResource extends AbstractResource
{
	private final CarAccessService carAccessService;

	@Inject
	public CarInvitationResource(SecurityIdentity identity, AccountRepository accountRepository,
			CarAccessService carAccessService)
	{
		super(identity, accountRepository);
		this.carAccessService = carAccessService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getInvitations", description = "The invitations to cars the user hasn't answered yet, the oldest first.")
	public List<CarInvitation> getInvitations()
	{
		return carAccessService.invitations(context.getName());
	}

	@POST
	@Path("{id}/accept")
	@Operation(operationId = "acceptInvitation", description = "The car shows for the user within the period of the access.")
	@APIResponse(responseCode = "204")
	@APIResponse(responseCode = "404", description = "Invitation not found for your user.")
	public void acceptInvitation(@PathParam("id") long id)
	{
		carAccessService.answer(id, true, context.getName());
	}

	@POST
	@Path("{id}/decline")
	@Operation(operationId = "declineInvitation", description = "The owner sees the invitation declined, a new one asks again.")
	@APIResponse(responseCode = "204")
	@APIResponse(responseCode = "404", description = "Invitation not found for your user.")
	public void declineInvitation(@PathParam("id") long id)
	{
		carAccessService.answer(id, false, context.getName());
	}
}
