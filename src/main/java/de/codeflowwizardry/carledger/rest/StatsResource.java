package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.StatsCalculator;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.records.stats.AverageStats;
import de.codeflowwizardry.carledger.rest.records.stats.HiLoStats;
import de.codeflowwizardry.carledger.rest.records.stats.MinimalStats;
import de.codeflowwizardry.carledger.rest.records.stats.TotalStats;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.time.LocalDate;
import java.util.Optional;

@Authenticated
@Path("stats/{carId}")
@ApplicationScoped
public class StatsResource extends AbstractResource
{
	private final StatsCalculator statsCalculator;

	/**
	 * CDI proxying
	 */
	public StatsResource()
	{
		super(null, null);
		statsCalculator = null;
	}

	@Inject
	public StatsResource(CurrentIdentityAssociation principal, AccountRepository accountRepository,
			StatsCalculator statsCalculator)
	{
		super(principal, accountRepository);
		this.statsCalculator = statsCalculator;
	}

	@GET
	@Path("total")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getStatsTotal", description = "Gets the accumulated stats for Unit/Distance/Cost")
	public TotalStats getTotal(@BeanParam DefaultParams params)
	{
		return statsCalculator.calculateTotal(params.carId, getName(), params.from, params.to);
	}

	@GET
	@Path("average")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getStatsAverage", description = "Gets the average stats for Distance/Cost/PricePerUnit/Fuel Consumption")
	public AverageStats getAverage(@BeanParam DefaultParams params)
	{
		return statsCalculator.calculateAverage(params.carId, getName(), params.from, params.to);
	}

	@GET
	@Path("hi_lo")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getStatsHiLo", description = "Gets the highes and lowest stats for Unit/Distance/Cost/PricePerUnit/Fuel Consumption")
	public HiLoStats getHiLo(@BeanParam DefaultParams params)
	{
		return statsCalculator.calculateHighLow(params.carId, getName(), params.from, params.to);
	}

	@GET
	@Path("minimal")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getStatsMinimal", description = "Gets a small amount of stats to show in a dashboard")
	public MinimalStats getMinimalStats(@BeanParam DefaultParams params)
	{
		return statsCalculator.getMinimalStats(params.carId, getName(), params.from, params.to);
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
