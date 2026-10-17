package de.codeflowwizardry.carledger.rest.car.recurring;

import io.quarkus.security.identity.SecurityIdentity;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.RecurringBillEntity;
import de.codeflowwizardry.carledger.data.factory.RecurringBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.RecurringBillRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("bill/{carId}/recurring")
public class RecurringResource extends AbstractResource
{
	private final static Logger LOG = LoggerFactory.getLogger(RecurringResource.class);

	private final RecurringBillRepository recurringBillRepository;
	private final RecurringBillFactory recurringBillFactory;

	@Inject
	public RecurringResource(SecurityIdentity identity, AccountRepository accountRepository,
			RecurringBillRepository recurringBillRepository, RecurringBillFactory recurringBillFactory)
	{
		super(identity, accountRepository);
		this.recurringBillRepository = recurringBillRepository;
		this.recurringBillFactory = recurringBillFactory;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getAllRecurringBills", description = "Gets all recurring bills for specified car")
	public BillPaged<RecurringBill> getAllRecurringBills(@PathParam("carId") long carId,
			@QueryParam("page") @DefaultValue("1") int page,
			@QueryParam("size") @DefaultValue("10") int size,
			@QueryParam("onlyRunning") boolean running,
			@QueryParam("sort") @DefaultValue("ACTIVE") RecurringBillSort sort)
	{
		page = toPageNumber(page);
		size = toPageSize(size);
		Page queryPage = new Page(page - 1, size);

		PanacheQuery<RecurringBillEntity> all = recurringBillRepository.getAll(carId, context.getName(), queryPage,
				running, sort == RecurringBillSort.NEXT_DUE);

		return new BillPaged<>(all.count(), page, size, RecurringBill.convert(all.list()));
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addNewRecurringBill")
	@APIResponse(responseCode = "202", content = @Content(schema = @Schema(implementation = RecurringBill.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	public Response addNewRecurringBill(@PathParam("carId") long carId, RecurringBillInput input)
	{
		RecurringBillEntity recurringBillEntity = recurringBillFactory.create(carId, input, context.getName());
		return Response.accepted(RecurringBill.convert(recurringBillEntity)).build();
	}

	@GET
	@Path("{billId: \\d+}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getRecurringBill", description = "Gets one recurring bill of the car")
	@APIResponse(responseCode = "200", description = "Bill found.")
	@APIResponse(responseCode = "404", description = "Bill not found for the car of your user.")
	public RecurringBill getRecurringBill(@PathParam("carId") long carId, @PathParam("billId") long billId)
	{
		RecurringBillEntity bill = recurringBillRepository.getById(carId, context.getName(), billId, Action.READ);
		if (bill == null)
		{
			throw new NotFoundException("Bill not found");
		}
		return RecurringBill.convert(bill);
	}

	@POST
	@Path("{billId: \\d+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateRecurringBill", description = "Updates a recurring bill, a changed amount, interval or date books the payments again")
	@APIResponse(responseCode = "200", description = "Bill updated.")
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Bill not found for the car of your user.")
	public RecurringBill updateRecurringBill(@PathParam("carId") long carId, @PathParam("billId") long billId,
			RecurringBillInput input)
	{
		return RecurringBill.convert(recurringBillFactory.update(carId, billId, input, context.getName()));
	}

	@DELETE
	@Path("{billId}")
	@Operation(operationId = "deleteRecurringBill")
	@APIResponse(responseCode = "202", description = "Successful deleted")
	@APIResponse(responseCode = "404", description = "Bill not found")
	public Response deleteRecurringBill(@PathParam("carId") long carId, @PathParam("billId") long billId)
	{
		boolean b = recurringBillRepository.deleteById(carId, billId, context.getName());
		if (b)
		{
			return Response.accepted().build();
		}
		LOG.warn("Could not delete bill {}", billId);
		return Response.status(404).build();
	}

	@POST
	@Path("{billId}/end")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "endRecurringBill", description = "Ends a recurring bill, payments due on or after the end date are removed")
	@APIResponse(responseCode = "200", description = "Bill ended.")
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Bill not found for the car of your user.")
	public RecurringBill endRecurringBill(@PathParam("carId") long carId, @PathParam("billId") long billId,
			RecurringBillEndInput input)
	{
		return RecurringBill.convert(recurringBillFactory.end(carId, billId, input.endDate(), context.getName()));
	}
}
