package de.codeflowwizardry.carledger.rest.car.recurring;

import java.security.Principal;
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
import de.codeflowwizardry.carledger.data.repository.RecurringBillRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.AbstractResource;
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
	public RecurringResource(Principal context, AccountRepository accountRepository,
			RecurringBillRepository recurringBillRepository, RecurringBillFactory recurringBillFactory)
	{
		super(context, accountRepository);
		this.recurringBillRepository = recurringBillRepository;
		this.recurringBillFactory = recurringBillFactory;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getAllRecurringBills", description = "Gets all recurring bills for specified car")
	public List<RecurringBill> getAllRecurringBills(@PathParam("carId") long carId)
	{
		return RecurringBill.convert(recurringBillRepository.getAll(carId, context.getName()));
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addNewRecurringBill")
	@APIResponse(responseCode = "202", content = @Content(schema = @Schema(implementation = RecurringBill.class)))
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	public Response addNewRecurringBill(@PathParam("carId") long carId, RecurringBillInput input)
	{
		try
		{
			RecurringBillEntity recurringBillEntity = recurringBillFactory.create(carId, context.getName(), input);
			return Response.accepted(RecurringBill.convert(recurringBillEntity)).build();
		}
		catch (WrongUserException e)
		{
			LOG.warn("Car cannot be found under your user {}!", context.getName(), e);
			throw new WebApplicationException(Response.status(Response.Status.CONFLICT)
					.entity("Car cannot be found under your user!")
					.build());
		}
	}

	@DELETE
	@Path("{billId}")
	@Operation(operationId = "deleteRecurringBill")
	@APIResponse(responseCode = "202", description = "Successful deleted")
	@APIResponse(responseCode = "400", description = "Not able to delete")
	public Response deleteRecurringBill(@PathParam("carId") long carId, @PathParam("billId") long billId)
	{
		boolean b = recurringBillRepository.deleteById(carId, context.getName(), billId);
		if (b)
		{
			return Response.accepted().build();
		}
		LOG.warn("Could not delete bill {}", billId);
		return Response.status(400).build();
	}
}
