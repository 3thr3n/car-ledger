package de.codeflowwizardry.carledger.rest.car.miscellaneous;

import java.security.Principal;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.MiscellaneousBillEntity;
import de.codeflowwizardry.carledger.data.factory.MiscellaneousBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.MiscellaneousBillRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.car.AbstractBillResource;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import de.codeflowwizardry.carledger.rest.records.MiscellaneousBill;
import de.codeflowwizardry.carledger.rest.records.input.MiscellaneousBillInput;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("bill/{carId}/miscellaneous")
public class MiscellaneousResource
		extends AbstractBillResource<MiscellaneousBillRepository, MiscellaneousBillEntity, MiscellaneousBill>
{
	private final static Logger LOG = LoggerFactory.getLogger(MiscellaneousResource.class);

	private final MiscellaneousBillFactory miscellaneousBillFactory;

	protected MiscellaneousResource(Principal context, AccountRepository accountRepository,
			MiscellaneousBillRepository miscellaneousBillRepository, MiscellaneousBillFactory miscellaneousBillFactory)
	{
		super(context, accountRepository, miscellaneousBillRepository, BillType.MISCELLANEOUS);
		this.miscellaneousBillFactory = miscellaneousBillFactory;
	}

	@GET
	@Path("years")
	@Operation(operationId = "getAllMiscellaneousBillYears", description = "Gets all years of bills for specified car")
	@APIResponse(responseCode = "200", description = "Bills found and years extracted.")
	public List<Integer> getAllMyBills(@PathParam("carId") long carId)
	{
		return super.getAllMyBillsYears(carId);
	}

	@GET
	@Path("all")
	@Operation(operationId = "getAllMiscellaneousBills", description = "Gets all bills for specified car, Pages starting at 1")
	@APIResponse(responseCode = "200", description = "Bills found.")
	public BillPaged<MiscellaneousBill> getAllMyBills(@PathParam("carId") long carId,
			@QueryParam("page") @DefaultValue("1") int page,
			@QueryParam("size") @DefaultValue("10") int size,
			@QueryParam("year") Integer year)
	{
		return super.getAllMyBills(carId, page, size, year, MiscellaneousBill::convert);
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addNewMiscellaneousBill")
	@APIResponse(responseCode = "200", description = "Bill created.")
	@APIResponse(responseCode = "400", description = "Car is not for your user.")
	@APIResponse(responseCode = "500", description = "Something went wrong while saving. Please ask the server admin for help.")
	public Response addNewBill(@PathParam("carId") long carId, MiscellaneousBillInput miscellaneousBillInput)
	{
		try
		{
			MiscellaneousBillEntity fuelBill = miscellaneousBillFactory.create(miscellaneousBillInput, carId,
					context.getName());
			return Response.accepted(MiscellaneousBill.convert(fuelBill)).build();
		}
		catch (WrongUserException e)
		{
			LOG.warn("Car cannot be found under your user {}!", context.getName(), e);
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
					.entity("Car cannot be found under your user!")
					.build());
		}
	}
}
