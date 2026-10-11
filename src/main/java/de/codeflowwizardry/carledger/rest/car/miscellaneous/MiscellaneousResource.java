package de.codeflowwizardry.carledger.rest.car.miscellaneous;

import io.quarkus.security.identity.SecurityIdentity;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.MiscellaneousBillEntity;
import de.codeflowwizardry.carledger.data.factory.MiscellaneousBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.MiscellaneousBillRepository;
import de.codeflowwizardry.carledger.rest.car.AbstractBillResource;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("bill/{carId}/miscellaneous")
public class MiscellaneousResource
		extends AbstractBillResource<MiscellaneousBillRepository, MiscellaneousBillEntity, MiscellaneousBill>
{

	private final MiscellaneousBillFactory miscellaneousBillFactory;

	@Inject
	public MiscellaneousResource(SecurityIdentity identity, AccountRepository accountRepository,
			MiscellaneousBillRepository miscellaneousBillRepository, MiscellaneousBillFactory miscellaneousBillFactory)
	{
		super(identity, accountRepository, miscellaneousBillRepository, BillType.MISCELLANEOUS);
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
	@APIResponse(responseCode = "202", description = "Bill created.")
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "500", description = "Something went wrong while saving. Please ask the server admin for help.")
	public Response addNewBill(@PathParam("carId") long carId, MiscellaneousBillInput miscellaneousBillInput)
	{
		return Response
				.accepted(MiscellaneousBill
						.convert(miscellaneousBillFactory.create(miscellaneousBillInput, carId, context.getName())))
				.build();
	}

	@GET
	@Path("{billId: \\d+}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getMiscellaneousBill", description = "Gets one bill of the car")
	@APIResponse(responseCode = "200", description = "Bill found.")
	@APIResponse(responseCode = "404", description = "Bill not found for the car of your user.")
	public MiscellaneousBill getBill(@PathParam("carId") long carId, @PathParam("billId") long billId)
	{
		return MiscellaneousBill.convert(findMyBill(carId, billId));
	}

	@POST
	@Path("{billId: \\d+}")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateMiscellaneousBill", description = "Updates a bill, calculated values are calculated again")
	@APIResponse(responseCode = "200", description = "Bill updated.")
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Bill not found for the car of your user.")
	@APIResponse(responseCode = "409", description = "Another bill with the same date and total exists.")
	public MiscellaneousBill updateBill(@PathParam("carId") long carId, @PathParam("billId") long billId,
			MiscellaneousBillInput input)
	{
		return MiscellaneousBill.convert(miscellaneousBillFactory.update(findMyBill(carId, billId), input));
	}
}
