package de.codeflowwizardry.carledger.rest.car;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;

import de.codeflowwizardry.carledger.StatsCalculator;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.records.stats.DashboardStats;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("stats/{carId}")
public class StatsResource extends AbstractResource
{
	private final StatsCalculator statsCalculator;

	@Inject
	public StatsResource(Principal context, AccountRepository accountRepository, StatsCalculator statsCalculator)
	{
		super(context, accountRepository);
		this.statsCalculator = statsCalculator;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getDashboardStats")
	public DashboardStats getStats(@BeanParam DefaultParams params)
	{
		return new DashboardStats(
				statsCalculator.calculateTotal(params.carId, context.getName(), params.from, params.to),
				statsCalculator.calculateAverage(params.carId, context.getName(), params.from, params.to),
				statsCalculator.calculateHighLow(params.carId, context.getName(), params.from, params.to));
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
