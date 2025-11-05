package de.codeflowwizardry.carledger.rest;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.Bill;
import de.codeflowwizardry.carledger.data.Car;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.records.BillInputPojo;
import de.codeflowwizardry.carledger.rest.records.BillPojo;
import de.codeflowwizardry.carledger.rest.records.BillPojoPaged;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("bill/{carId}")
public class BillResource extends AbstractResource
{
	private final BillRepository billRepository;
	private final CarRepository carRepository;

	@Inject
	public BillResource(Principal context, AccountRepository accountRepository, BillRepository billRepository,
			CarRepository carRepository)
	{
		super(context, accountRepository);
		this.billRepository = billRepository;
		this.carRepository = carRepository;
	}

    @GET
    @Path("years")
    @Operation(operationId = "getAllBillYears", description = "Gets all years of bills for specified car")
    @APIResponse(responseCode = "200", description = "Bills found and years extracted.")
    public List<Integer> getAllMyBills(@PathParam("carId") long carId)
    {
        return billRepository.getBillYears(carId, context.getName());
    }

	@GET
	@Path("all")
	@Operation(operationId = "getAllBills", description = "Gets all bills for specified car")
	@APIResponse(responseCode = "200", description = "Bills found.")
	public BillPojoPaged getAllMyBills(@PathParam("carId") long carId,
			@QueryParam("page") @DefaultValue("1") int page,
			@QueryParam("size") @DefaultValue("10") int size,
			@QueryParam("year") Integer year)
	{
		if (page < 1)
		{
			page = 1;
		}
		Page queryPage = new Page(page - 1, size);

		PanacheQuery<Bill> billQuery = billRepository.getBills(carId, context.getName(), queryPage, Optional.ofNullable(year));
		return new BillPojoPaged(billQuery.count(), page, size, BillPojo.convert(billQuery.list()));
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "addNewBill")
	@APIResponse(responseCode = "200", description = "Bill created.")
	@APIResponse(responseCode = "400", description = "Car is not for your user.")
	@APIResponse(responseCode = "500", description = "Something went wrong while saving. Please ask the server admin for help.")
	public Response addNewBill(@PathParam("carId") long carId, BillInputPojo billPojo)
	{
		Car car = carRepository.findById(carId, context.getName());

		if (car == null)
		{
			throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
					.entity("Car cannot be found under your user!")
					.build());
		}

		Bill bill = new Bill(billPojo);
		bill.setCar(car);
		billRepository.persist(bill);

		if (!billRepository.isPersistent(bill))
		{
			throw new InternalServerErrorException(Response.status(Response.Status.BAD_REQUEST)
					.entity("Could not save Bill")
					.build());
		}

		return Response.accepted(BillPojo.convert(bill)).build();
	}

	@DELETE
	@Path("{billId}")
	@Operation(operationId = "deleteBill")
	@APIResponse(responseCode = "202", description = "Bill deleted.")
	@APIResponse(responseCode = "400", description = "Bill not found on the specified car.")
	@APIResponse(responseCode = "500", description = "Something went wrong while deleting. Please ask the server admin for help.")
	public Response deleteBill(@PathParam("carId") long carId, @PathParam("billId") long billId)
	{
		Optional<Bill> optBill = billRepository.getBillById(billId, carId, context.getName());
		if (optBill.isEmpty())
		{
			throw new BadRequestException("Bill with id " + billId + " not found on car!");
		}
		Bill bill = optBill.get();

		billRepository.delete(bill);

		if (billRepository.isPersistent(bill))
		{
			throw new InternalServerErrorException("Could not delete bill!");
		}

		return Response.accepted().build();
	}
}
