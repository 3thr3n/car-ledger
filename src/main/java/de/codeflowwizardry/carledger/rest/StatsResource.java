package de.codeflowwizardry.carledger.rest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.Bill;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import io.quarkus.panache.common.Page;
import jakarta.inject.Inject;
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
	public String getAllStats(@PathParam("carId") Long carId,
			@QueryParam("start") Optional<LocalDate> start,
			@QueryParam("end") Optional<LocalDate> end)
	{
		Page page = new Page(0, 999);
		List<Bill> list = billRepository.getBills(carId, context.getName(), page).list();

		BigDecimal totalDistance = list.stream().map(Bill::getDistance).reduce(BigDecimal.ZERO, BigDecimal::add);
		return totalDistance.setScale(2, RoundingMode.HALF_UP).toString();
	}
}
