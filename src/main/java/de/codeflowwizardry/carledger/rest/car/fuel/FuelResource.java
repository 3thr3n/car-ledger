package de.codeflowwizardry.carledger.rest.car.fuel;

import io.quarkus.security.identity.SecurityIdentity;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.FuelBillRepository;
import de.codeflowwizardry.carledger.rest.car.AbstractBillResource;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("bill/{carId}/fuel")
public class FuelResource extends AbstractBillResource<FuelBillRepository, FuelBillEntity, FuelBill>
{

	private final FuelBillFactory fuelBillFactory;

	@Inject
	public FuelResource(SecurityIdentity identity, AccountRepository accountRepository,
			FuelBillRepository fuelBillRepository, FuelBillFactory fuelBillFactory)
	{
		super(identity, accountRepository, fuelBillRepository, BillType.FUEL);
		this.fuelBillFactory = fuelBillFactory;
	}

	@GET
	@Path("years")
	@Operation(operationId = "getAllFuelBillYears", description = "Gets all years of bills for specified car")
	@APIResponse(responseCode = "200", description = "Bills found and years extracted.")
	public List<Integer> getAllMyBills(@PathParam("carId") long carId)
	{
		return super.getAllMyBillsYears(carId);
	}

	@GET
	@Path("all")
	@Operation(operationId = "getAllFuelBills", description = "Gets all bills for specified car, Pages starting at 1")
	@APIResponse(responseCode = "200", description = "Bills found.")
	public BillPaged<FuelBill> getAllMyBills(@PathParam("carId") long carId,
			@QueryParam("page") @DefaultValue("1") int page,
			@QueryParam("size") @DefaultValue("10") int size,
			@QueryParam("year") Integer year)
	{
		return super.getAllMyBills(carId, page, size, year, FuelBill::convert);
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addNewFuelBill")
	@APIResponse(responseCode = "202", description = "Bill created.")
	@APIResponse(responseCode = "404", description = "Car is not for your user.")
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	@APIResponse(responseCode = "500", description = "Something went wrong while saving. Please ask the server admin for help.")
	public Response addNewBill(@PathParam("carId") long carId, FuelBillInput fuelBillPojo)
	{
		return Response.accepted(FuelBill.convert(fuelBillFactory.create(fuelBillPojo, carId, context.getName())))
				.build();
	}
}
