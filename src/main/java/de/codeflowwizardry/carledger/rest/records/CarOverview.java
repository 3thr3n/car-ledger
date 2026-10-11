package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.util.List;

import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.repository.BillRepository.BillTypeSummary;

public record CarOverview(long totalRefuels, long totalMaintenanceEvents, long totalMiscellaneousEvents,
		BigDecimal totalCost, BigDecimal avgConsumption)
{
	public static CarOverview convert(List<BillTypeSummary> summaries, BigDecimal avgConsumption)
	{
		BigDecimal totalCost = summaries.stream()
				.map(BillTypeSummary::total)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return new CarOverview(count(summaries, BillType.FUEL), count(summaries, BillType.MAINTENANCE),
				count(summaries, BillType.MISCELLANEOUS), totalCost, avgConsumption);
	}

	private static long count(List<BillTypeSummary> summaries, BillType type)
	{
		return summaries.stream()
				.filter(summary -> summary.type() == type)
				.mapToLong(BillTypeSummary::count)
				.sum();
	}
}
