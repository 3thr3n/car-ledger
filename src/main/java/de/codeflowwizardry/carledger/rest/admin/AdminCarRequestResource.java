package de.codeflowwizardry.carledger.rest.admin;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.request.CarLimitRequests;
import de.codeflowwizardry.carledger.request.RequestEntity;
import de.codeflowwizardry.carledger.request.RequestRepository;
import de.codeflowwizardry.carledger.request.RequestService;
import de.codeflowwizardry.carledger.request.RequestState;
import de.codeflowwizardry.carledger.request.RequestType;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * The requests for more cars of all accounts. Only for the role "admin",
 * enforced by the HTTP permission of "/api/admin*".
 */
@Path("admin/car-requests")
@Produces(MediaType.APPLICATION_JSON)
public class AdminCarRequestResource extends AbstractResource
{
	private final RequestRepository requestRepository;
	private final RequestService requestService;
	private final CarLimitRequests carLimitRequests;
	private final CarRepository carRepository;

	@Inject
	public AdminCarRequestResource(SecurityIdentity identity, AccountRepository accountRepository,
			RequestRepository requestRepository, RequestService requestService, CarLimitRequests carLimitRequests,
			CarRepository carRepository)
	{
		super(identity, accountRepository);
		this.requestRepository = requestRepository;
		this.requestService = requestService;
		this.carLimitRequests = carLimitRequests;
		this.carRepository = carRepository;
	}

	@GET
	@Operation(operationId = "getAdminCarRequests", description = "Requests for more cars, the open ones by age, the decided ones newest first; pages starting at 1")
	@APIResponse(responseCode = "200", description = "Requests found.")
	public AdminCarRequestPage getCarRequests(
			@QueryParam("state") RequestState state,
			@QueryParam("page") @DefaultValue("1") int page,
			@QueryParam("size") @DefaultValue("20") int size)
	{
		page = toPageNumber(page);
		size = toPageSize(size);
		List<AdminCarRequest> requests = requestRepository.list(RequestType.CAR_LIMIT, state, page - 1, size)
				.stream()
				.map(this::convert)
				.toList();
		return new AdminCarRequestPage(requestRepository.count(RequestType.CAR_LIMIT, state), page, size,
				requests);
	}

	@POST
	@Path("{id}/approve")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "approveCarRequest", description = "Approves the request and raises the car limit of the account")
	@APIResponse(responseCode = "200", description = "Request approved.")
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Request not found.")
	@APIResponse(responseCode = "409", description = "The request was decided already.")
	public AdminCarRequest approve(@PathParam("id") long id, ApproveCarRequestInput input)
	{
		return convert(requestService.approve(id, carLimitRequests, input == null ? null : input.maxCars(),
				context.getName()));
	}

	@POST
	@Path("{id}/reject")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "rejectCarRequest", description = "Rejects the request, the comment is shown to the user")
	@APIResponse(responseCode = "200", description = "Request rejected.")
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Request not found.")
	@APIResponse(responseCode = "409", description = "The request was decided already.")
	public AdminCarRequest reject(@PathParam("id") long id, RejectCarRequestInput input)
	{
		return convert(requestService.reject(id, RequestType.CAR_LIMIT, input == null ? null : input.comment(),
				context.getName()));
	}

	private AdminCarRequest convert(RequestEntity request)
	{
		return AdminCarRequest.convert(request, carRepository.count("user", request.getAccount()));
	}
}
