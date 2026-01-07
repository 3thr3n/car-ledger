package de.codeflowwizardry.carledger.rest.car;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;

import de.codeflowwizardry.carledger.StatsCalculator;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import de.codeflowwizardry.carledger.rest.records.stats.AverageStats;
import de.codeflowwizardry.carledger.rest.records.stats.HiLoStats;
import de.codeflowwizardry.carledger.rest.records.stats.TotalStats;
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
	@Path("total")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getStatsTotal", description = "Gets the accumulated stats for Unit/Distance/Cost")
	public TotalStats getTotal(@BeanParam DefaultParams params)
	{
		return statsCalculator.calculateTotal(params.carId, context.getName(), params.from, params.to);
	}

	@GET
	@Path("average")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getStatsAverage", description = "Gets the average stats for Distance/Cost/PricePerUnit/Fuel Consumption")
	public AverageStats getAverage(@BeanParam DefaultParams params)
	{
		return statsCalculator.calculateAverage(params.carId, context.getName(), params.from, params.to);
	}

	@GET
	@Path("hi_lo")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getStatsHiLo", description = "Gets the highes and lowest stats for Unit/Distance/Cost/PricePerUnit/Fuel Consumption")
	public HiLoStats getHiLo(@BeanParam DefaultParams params)
	{
		return statsCalculator.calculateHighLow(params.carId, context.getName(), params.from, params.to);
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
