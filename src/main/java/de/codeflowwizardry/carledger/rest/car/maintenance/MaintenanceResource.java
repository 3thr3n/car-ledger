package de.codeflowwizardry.carledger.rest.car.maintenance;

import io.quarkus.security.identity.SecurityIdentity;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.MaintenanceBillEntity;
import de.codeflowwizardry.carledger.data.factory.MaintenanceBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.MaintenanceBillRepository;
import de.codeflowwizardry.carledger.rest.car.AbstractBillResource;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("bill/{carId}/maintenance")
public class MaintenanceResource
		extends AbstractBillResource<MaintenanceBillRepository, MaintenanceBillEntity, MaintenanceBill>
{

	private final MaintenanceBillFactory maintenanceBillFactory;

	@Inject
	public MaintenanceResource(SecurityIdentity identity, AccountRepository accountRepository,
			MaintenanceBillRepository maintenanceBillRepository, MaintenanceBillFactory maintenanceBillFactory)
	{
		super(identity, accountRepository, maintenanceBillRepository, BillType.MAINTENANCE);
		this.maintenanceBillFactory = maintenanceBillFactory;
	}

	@GET
	@Path("years")
	@Operation(operationId = "getAllMaintenanceBillYears", description = "Gets all years of bills for specified car")
	@APIResponse(responseCode = "200", description = "Bills found and years extracted.")
	public List<Integer> getAllMyBills(@PathParam("carId") long carId)
	{
		return super.getAllMyBillsYears(carId);
	}

	@GET
	@Path("all")
	@Operation(operationId = "getAllMaintenanceBills", description = "Gets all bills for specified car, Pages starting at 1")
	@APIResponse(responseCode = "200", description = "Bills found.")
	public BillPaged<MaintenanceBill> getAllMyBills(@PathParam("carId") long carId,
			@QueryParam("page") @DefaultValue("1") int page,
			@QueryParam("size") @DefaultValue("10") int size,
			@QueryParam("year") Integer year)
	{
		return super.getAllMyBills(carId, page, size, year, MaintenanceBill::convert);
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addNewMaintenanceBill")
	@APIResponse(responseCode = "202", description = "Bill created.")
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "500", description = "Something went wrong while saving. Please ask the server admin for help.")
	public Response addNewBill(@PathParam("carId") long carId, MaintenanceBillInput maintenanceBillInput)
	{
		return Response
				.accepted(MaintenanceBill
						.convert(maintenanceBillFactory.create(maintenanceBillInput, carId, context.getName())))
				.build();
	}

	@GET
	@Path("{billId: \\d+}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getMaintenanceBill", description = "Gets one bill of the car")
	@APIResponse(responseCode = "200", description = "Bill found.")
	@APIResponse(responseCode = "404", description = "Bill not found for the car of your user.")
	public MaintenanceBill getBill(@PathParam("carId") long carId, @PathParam("billId") long billId)
	{
		return MaintenanceBill.convert(findMyBill(carId, billId));
	}

	@POST
	@Path("{billId: \\d+}")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateMaintenanceBill", description = "Updates a bill, calculated values are calculated again")
	@APIResponse(responseCode = "200", description = "Bill updated.")
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "404", description = "Bill not found for the car of your user.")
	@APIResponse(responseCode = "409", description = "Another bill with the same date and total exists.")
	public MaintenanceBill updateBill(@PathParam("carId") long carId, @PathParam("billId") long billId,
			MaintenanceBillInput input)
	{
		return MaintenanceBill.convert(maintenanceBillFactory.update(findMyBill(carId, billId), input));
	}
}
