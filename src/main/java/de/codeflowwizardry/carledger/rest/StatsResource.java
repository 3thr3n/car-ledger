package de.codeflowwizardry.carledger.rest;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Optional;

import de.codeflowwizardry.carledger.StatsCalculator;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.records.stats.AverageStats;
import de.codeflowwizardry.carledger.rest.records.stats.TotalStats;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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
	@Path("total")
	@Produces(MediaType.APPLICATION_JSON)
	public TotalStats getTotal(@BeanParam DefaultParams params)
	{
		return statsCalculator.calculateTotal(params.carId, context.getName(), params.from, params.to);
	}

	@GET
	@Path("average")
	@Produces(MediaType.APPLICATION_JSON)
	public AverageStats getAverage(@BeanParam DefaultParams params)
	{
		return statsCalculator.calculateAverage(params.carId, context.getName(), params.from, params.to);
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
