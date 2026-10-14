package de.codeflowwizardry.carledger.rest.request;

import java.net.URI;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.request.CarLimitRequests;
import de.codeflowwizardry.carledger.request.RequestRepository;
import de.codeflowwizardry.carledger.request.RequestService;
import de.codeflowwizardry.carledger.request.RequestType;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The own requests for more cars
 */
@Path("user/me/car-requests")
@Produces(MediaType.APPLICATION_JSON)
public class CarRequestResource extends AbstractResource
{
	private final CarLimitRequests carLimitRequests;
	private final RequestService requestService;
	private final RequestRepository requestRepository;

	@Inject
	public CarRequestResource(SecurityIdentity identity, AccountRepository accountRepository,
			CarLimitRequests carLimitRequests, RequestService requestService, RequestRepository requestRepository)
	{
		super(identity, accountRepository);
		this.carLimitRequests = carLimitRequests;
		this.requestService = requestService;
		this.requestRepository = requestRepository;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "requestMoreCars", description = "Requests more cars than the limit of the account allows")
	@APIResponse(responseCode = "201", description = "Request created.", content = @Content(schema = @Schema(implementation = CarRequest.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, or the user can still add cars", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "409", description = "An open request exists already.")
	public Response requestMoreCars(CarRequestInput input)
	{
		var request = carLimitRequests.request(getAccount(), input.cars(), input.reason());
		return Response.created(URI.create("user/me/car-requests/" + request.getId()))
				.entity(CarRequest.convert(request))
				.build();
	}

	@GET
	@Path("latest")
	@Operation(operationId = "getMyLatestCarRequest", description = "The newest own request for more cars")
	@APIResponse(responseCode = "200", description = "Request found.", content = @Content(schema = @Schema(implementation = CarRequest.class)))
	@APIResponse(responseCode = "204", description = "No request yet.")
	public Response getLatest()
	{
		return requestRepository.latest(getAccount(), RequestType.CAR_LIMIT)
				.map(request -> Response.ok(CarRequest.convert(request)).build())
				.orElseGet(() -> Response.noContent().build());
	}

	@POST
	@Path("{id}/seen")
	@Operation(operationId = "markCarRequestSeen", description = "The decision was shown to the user, it is not shown again")
	@APIResponse(responseCode = "204", description = "Marked as seen.")
	@APIResponse(responseCode = "404", description = "Request not found for the user.")
	public Response markSeen(@PathParam("id") long id)
	{
		requestService.markSeen(id, getAccount());
		return Response.noContent().build();
	}
}
