package de.codeflowwizardry.carledger.rest.car.recurring;

import java.security.Principal;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;

import de.codeflowwizardry.carledger.data.RecurringBillEntity;
import de.codeflowwizardry.carledger.data.factory.RecurringBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.RecurringBillRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("recurring/{carId}")
public class RecurringResource extends AbstractResource
{
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
	@Operation(operationId = "addNewMiscellaneousBill")
	public Response createRecurringBill(@PathParam("carId") long carId, RecurringBillInput input)
	{
		RecurringBillEntity recurringBillEntity = recurringBillFactory.create(carId, context.getName(), input);
		return Response.accepted(RecurringBill.convert(recurringBillEntity)).build();
	}
}
