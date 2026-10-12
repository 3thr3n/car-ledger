package de.codeflowwizardry.carledger.rest.car.stats;

import io.quarkus.security.identity.SecurityIdentity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;

import de.codeflowwizardry.carledger.StatsCalculator;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("stats/{carId}")
public class StatsResource extends AbstractResource
{
	private final StatsCalculator statsCalculator;

	@Inject
	public StatsResource(SecurityIdentity identity, AccountRepository accountRepository,
			StatsCalculator statsCalculator)
	{
		super(identity, accountRepository);
		this.statsCalculator = statsCalculator;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getDashboardStats", description = "Stats of a car, use -1 as carId for all your cars")
	public DashboardStats getStats(@BeanParam DefaultParams params)
	{
		return statsCalculator.calculateStats(params.carId, context.getName(), params.from, params.to);
	}

	@GET
	@Path("monthly")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getMonthlyStats", description = "Costs and fuel per month of a car, use -1 as carId for all your cars. "
			+ "Every month of the range is included, without from/to it starts/ends with the first/last month with bills.")
	public List<MonthStats> getMonthlyStats(@BeanParam DefaultParams params)
	{
		return statsCalculator.calculateMonthly(params.carId, context.getName(), params.from, params.to);
	}

	public static class DefaultParams
	{
		@PathParam("carId")
		Long carId;

		@QueryParam("from")
		Optional<LocalDate> from;

		@QueryParam("to")
		Optional<LocalDate> to;
	}
}
