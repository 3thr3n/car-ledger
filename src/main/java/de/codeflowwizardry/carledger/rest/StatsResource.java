package de.codeflowwizardry.carledger.rest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.Bill;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

@Path("stats/{carId}")
public class StatsResource extends AbstractResource
{
	private final BillRepository billRepository;

	@Inject
	public StatsResource(BillRepository billRepository)
	{
		this.billRepository = billRepository;
	}

	@GET
	public String getTotalDistance(@BeanParam DefaultParams params)
	{
		List<Bill> list = billRepository.getBills(params.carId, context.getName(), params.from, params.to);

		BigDecimal totalDistance = list.stream().map(Bill::getDistance).reduce(BigDecimal.ZERO, BigDecimal::add);
		return totalDistance.setScale(2, RoundingMode.HALF_UP).toString();
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
