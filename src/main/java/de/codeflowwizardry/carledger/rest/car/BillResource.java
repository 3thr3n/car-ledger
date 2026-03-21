package de.codeflowwizardry.carledger.rest.car;

import java.security.Principal;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.exception.DbDeletionException;
import de.codeflowwizardry.carledger.rest.records.Bill;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("bill/{carId}")
public class BillResource extends AbstractBillResource<BillRepository, BillEntity, Bill>
{
	@Inject
	public BillResource(Principal context, AccountRepository accountRepository, BillRepository billRepository)
	{
		super(context, accountRepository, billRepository, null);
	}

	@GET
	@Override
	@Path("years")
	@Operation(operationId = "getAllBillYears", description = "Gets all years of bills for specified car")
	@APIResponse(responseCode = "200", description = "Bills found and years extracted.")
	public List<Integer> getAllMyBillsYears(@PathParam("carId") long carId)
	{
		return super.getAllMyBillsYears(carId);
	}

	@DELETE
	@Path("{billId}")
	@Operation(operationId = "deleteBill", description = "Deletes the bill")
	@APIResponse(responseCode = "204", description = "Bill deleted.")
	public void deleteBill(@PathParam("carId") long carId, @PathParam("billId") long billId)
	{
		try
		{
			repository.delete(carId, billId, context.getName());
		}
		catch (DbDeletionException e)
		{
			throw new WebApplicationException(Response
					.status(400)
					.entity("Bill could not be found! Either not connected to your user or already deleted.")
					.build());
		}
	}

	@GET
	@Path("all")
	@Operation(operationId = "getAllBills", description = "Gets all bills for specified car, Pages starting at 1")
	@APIResponse(responseCode = "200", description = "Bills found.")
	public BillPaged<Bill> getAllMyBills(@PathParam("carId") long carId,
			@QueryParam("page") @DefaultValue("1") int page,
			@QueryParam("size") @DefaultValue("10") int size,
			@QueryParam("year") Integer year)
	{
		return super.getAllMyBills(carId, page, size, year, Bill::convert);
	}
}
