package de.codeflowwizardry.carledger.rest.car;

import java.security.Principal;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.records.Bill;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

@Path("bill/{carId}")
public class BillResource extends AbstractResource
{
	private final BillRepository billRepository;

	@Inject
	public BillResource(Principal context, AccountRepository accountRepository, BillRepository billRepository)
	{
		super(context, accountRepository);
		this.billRepository = billRepository;
	}

	@GET
	@Path("years")
	@Operation(operationId = "getAllBillYears", description = "Gets all years of bills for specified car")
	@APIResponse(responseCode = "200", description = "Bills found and years extracted.")
	public List<Integer> getAllMyBillsYears(@PathParam("carId") long carId)
	{
		return billRepository.getBillYears(carId, context.getName());
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
		if (page < 1)
		{
			page = 1;
		}

		Page queryPage = new Page(page - 1, size);

		PanacheQuery<BillEntity> byCarAndYear = billRepository.findByCarAndYear(carId, year, queryPage);

		return new BillPaged<>(byCarAndYear.count(), page, size, Bill.convert(byCarAndYear.list()));
	}
}
