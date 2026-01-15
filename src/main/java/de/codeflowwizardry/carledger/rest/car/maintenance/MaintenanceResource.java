package de.codeflowwizardry.carledger.rest.car.maintenance;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.MaintenanceBillEntity;
import de.codeflowwizardry.carledger.data.factory.MaintenanceBillFactory;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.MaintenanceBillRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import de.codeflowwizardry.carledger.rest.records.MaintenanceBill;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.ws.rs.*;

@Path("bill/{carId}/maintenance")
public class MaintenanceResource extends AbstractResource
{
	private final MaintenanceBillRepository maintenanceBillRepository;
	private final MaintenanceBillFactory maintenanceBillFactory;

	protected MaintenanceResource(Principal context, AccountRepository accountRepository,
			MaintenanceBillRepository maintenanceBillRepository, MaintenanceBillFactory maintenanceBillFactory)
	{
		super(context, accountRepository);
		this.maintenanceBillRepository = maintenanceBillRepository;
		this.maintenanceBillFactory = maintenanceBillFactory;
	}

	@GET
	@Path("years")
	@Operation(operationId = "getAllMaintenanceBillYears", description = "Gets all years of bills for specified car")
	@APIResponse(responseCode = "200", description = "Bills found and years extracted.")
	public List<Integer> getAllMyBills(@PathParam("carId") long carId)
	{
		return maintenanceBillRepository.getBillYears(carId, context.getName(), BillType.MAINTENANCE);
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
		if (page < 1)
		{
			page = 1;
		}
		Page queryPage = new Page(page - 1, size);

		PanacheQuery<MaintenanceBillEntity> billQuery = maintenanceBillRepository
				.getBills(carId, context.getName(), queryPage,
						Optional.ofNullable(year));
		return new BillPaged<>(billQuery.count(), page, size, MaintenanceBill.convert(billQuery.list()));
	}
}
