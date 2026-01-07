package de.codeflowwizardry.carledger.rest.car.fuel;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.FuelBillRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import de.codeflowwizardry.carledger.rest.records.FuelBill;
import de.codeflowwizardry.carledger.rest.records.input.FuelBillInput;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("bill/{carId}/fuel")
public class FuelResource extends AbstractResource
{
	private final static Logger LOG = LoggerFactory.getLogger(FuelResource.class);

	private final FuelBillRepository fuelBillRepository;
	private final FuelBillFactory fuelBillFactory;

	@Inject
	public FuelResource(Principal context, AccountRepository accountRepository,
			FuelBillRepository fuelBillRepository, FuelBillFactory fuelBillFactory)
	{
		super(context, accountRepository);
		this.fuelBillRepository = fuelBillRepository;
		this.fuelBillFactory = fuelBillFactory;
	}

	@GET
	@Path("years")
	@Operation(operationId = "getAllFuelBillYears", description = "Gets all years of bills for specified car")
	@APIResponse(responseCode = "200", description = "Bills found and years extracted.")
	public List<Integer> getAllMyBills(@PathParam("carId") long carId)
	{
		return fuelBillRepository.getBillYears(carId, context.getName(), BillType.FUEL);
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
		if (page < 1)
		{
			page = 1;
		}
		Page queryPage = new Page(page - 1, size);

		PanacheQuery<FuelBillEntity> billQuery = fuelBillRepository
				.getBills(carId, context.getName(), queryPage,
						Optional.ofNullable(year));
		return new BillPaged<>(billQuery.count(), page, size, FuelBill.convert(billQuery.list()));
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addNewBill")
	@APIResponse(responseCode = "200", description = "Bill created.")
	@APIResponse(responseCode = "400", description = "Car is not for your user.")
	@APIResponse(responseCode = "500", description = "Something went wrong while saving. Please ask the server admin for help.")
	public Response addNewBill(@PathParam("carId") long carId, FuelBillInput fuelBillPojo)
	{
		try
		{
			FuelBillEntity fuelBill = fuelBillFactory.create(fuelBillPojo, carId, context.getName());
			return Response.accepted(FuelBill.convert(fuelBill)).build();
		}
		catch (WrongUserException e)
		{
			LOG.warn("Car cannot be found under your user {}!", context.getName(), e);
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
					.entity("Car cannot be found under your user!")
					.build());
		}
	}

	@DELETE
	@Path("{billId}")
	@Operation(operationId = "deleteBill")
	@APIResponse(responseCode = "202", description = "Bill deleted.")
	@APIResponse(responseCode = "400", description = "Bill not found on the specified car.")
	@APIResponse(responseCode = "500", description = "Something went wrong while deleting. Please ask the server admin for help.")
	public Response deleteBill(@PathParam("carId") long carId, @PathParam("billId") long billId)
	{
		Optional<FuelBillEntity> optBill = fuelBillRepository.getBillById(billId, carId, context.getName());
		if (optBill.isEmpty())
		{
			throw new BadRequestException("Bill with id " + billId + " not found on car!");
		}
		FuelBillEntity billEntity = optBill.get();

		fuelBillRepository.delete(billEntity);

		return Response.accepted().build();
	}
}
