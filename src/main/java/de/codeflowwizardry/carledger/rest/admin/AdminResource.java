package de.codeflowwizardry.carledger.rest.admin;

import java.time.OffsetDateTime;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.AccountRepository.AccountUsage;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillRepository;
import de.codeflowwizardry.carledger.request.RequestRepository;
import de.codeflowwizardry.carledger.request.RequestState;
import de.codeflowwizardry.carledger.request.RequestType;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Administration of all accounts. Only for the role "admin", enforced by the
 * HTTP
 * permission of "/api/admin*" (a @RolesAllowed fails in dev mode, as its check
 * runs
 * without request context behind the static resources).
 */
@Path("admin")
// also checked by the HTTP permission of /api/admin*, in case it is ever
// changed
@RolesAllowed("admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource extends AbstractResource
{
	private final CarRepository carRepository;
	private final BillRepository billRepository;
	private final RecurringBillRepository recurringBillRepository;
	private final RequestRepository requestRepository;

	@Inject
	public AdminResource(SecurityIdentity identity, AccountRepository accountRepository, CarRepository carRepository,
			BillRepository billRepository, RecurringBillRepository recurringBillRepository,
			RequestRepository requestRepository)
	{
		super(identity, accountRepository);
		this.carRepository = carRepository;
		this.billRepository = billRepository;
		this.recurringBillRepository = recurringBillRepository;
		this.requestRepository = requestRepository;
	}

	@GET
	@Path("stats")
	@Operation(operationId = "getAdminStats", description = "Usage of the whole installation")
	@APIResponse(responseCode = "200", description = "Statistics calculated.")
	public AdminStats getStats()
	{
		return new AdminStats(
				accountRepository.count(),
				accountRepository.countSeenSince(OffsetDateTime.now().minusDays(30)),
				carRepository.count(),
				billRepository.count(),
				recurringBillRepository.count(),
				requestRepository.count(RequestType.CAR_LIMIT, RequestState.OPEN));
	}

	@GET
	@Path("accounts")
	@Operation(operationId = "getAdminAccounts", description = "All accounts with their usage, pages starting at 1")
	@APIResponse(responseCode = "200", description = "Accounts found.")
	public AdminAccountPage getAccounts(
			@QueryParam("page") @DefaultValue("1") int page,
			@QueryParam("size") @DefaultValue("20") int size,
			@QueryParam("search") String search)
	{
		page = toPageNumber(page);
		size = toPageSize(size);

		List<AdminAccount> accounts = accountRepository.listUsage(search, page - 1, size).stream()
				.map(AdminAccount::convert)
				.toList();

		return new AdminAccountPage(accountRepository.countMatching(search), page, size, accounts);
	}

	@POST
	@Path("accounts/{id}")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateAdminAccount", description = "Changes how many cars the account may have")
	@APIResponse(responseCode = "200", description = "Account updated.")
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Account not found.")
	public AdminAccount updateAccount(@PathParam("id") long id, MaxCarsInput input)
	{
		if (input.maxCars() == null || input.maxCars() < 0 || input.maxCars() > MaxCarsInput.LIMIT)
		{
			Utils.requireValid(List.of("Max cars must be between 0 and " + MaxCarsInput.LIMIT + "!"));
		}

		AccountEntity account = accountRepository.findByIdOptional(id)
				.orElseThrow(() -> new NotFoundException("Account not found"));
		account.setMaxCars(input.maxCars());

		long cars = carRepository.count("user", account);
		long bills = billRepository.count("car.user", account);
		return AdminAccount.convert(new AccountUsage(account, cars, bills));
	}

	@DELETE
	@Path("accounts/{id}")
	@Operation(operationId = "deleteAdminAccount", description = "Deletes the account with all its cars and bills")
	@APIResponse(responseCode = "204", description = "Account deleted.")
	@APIResponse(responseCode = "400", description = "The own account cannot be deleted.")
	@APIResponse(responseCode = "404", description = "Account not found.")
	public Response deleteAccount(@PathParam("id") long id)
	{
		AccountEntity account = accountRepository.findByIdOptional(id)
				.orElseThrow(() -> new NotFoundException("Account not found"));
		if (account.getUserId().equals(context.getName()))
		{
			return Response.status(400).entity(List.of("You cannot delete your own account!")).build();
		}

		accountRepository.deleteAccount(id);
		return Response.noContent().build();
	}
}
