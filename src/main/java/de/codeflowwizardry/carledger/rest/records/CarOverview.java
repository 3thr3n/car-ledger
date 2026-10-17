package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.util.List;

import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.repository.BillRepository.BillTypeSummary;

/**
 * @param avgConsumption
 *            liters per 100 km of the refuels
 * @param avgChargingConsumption
 *            kWh per 100 km of the charges
 */
public record CarOverview(long totalRefuels, long totalCharges, long totalMaintenanceEvents,
		long totalMiscellaneousEvents, BigDecimal totalCost, BigDecimal avgConsumption,
		BigDecimal avgChargingConsumption)
{
	/**
	 * @param recurringTotal
	 *            sum of the payments of the recurring costs, they are not part of
	 *            the summaries
	 */
	public static CarOverview convert(List<BillTypeSummary> summaries, BigDecimal recurringTotal,
			BigDecimal avgConsumption, BigDecimal avgChargingConsumption)
	{
		BigDecimal totalCost = summaries.stream()
				.map(BillTypeSummary::total)
				.reduce(recurringTotal, BigDecimal::add);
		return new CarOverview(count(summaries, BillType.FUEL), count(summaries, BillType.CHARGING),
				count(summaries, BillType.MAINTENANCE), count(summaries, BillType.MISCELLANEOUS), totalCost,
				avgConsumption, avgChargingConsumption);
	}

	private static long count(List<BillTypeSummary> summaries, BillType type)
	{
		return summaries.stream()
				.filter(summary -> summary.type() == type)
				.mapToLong(BillTypeSummary::count)
				.sum();
	}
}
