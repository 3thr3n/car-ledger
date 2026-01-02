package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.FuelBillEntity;

public record CarOverview(BigInteger totalRefuels, BigDecimal totalCost, BigDecimal avgConsumption)
{
	public static CarOverview convert(CarEntity carEntity)
	{
		List<BillEntity> billEntities = carEntity.getBills();

		BigDecimal totalCost = billEntities.stream().map(BillEntity::getTotal).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		// BigDecimal avgConsumption = BigDecimal.ZERO;
		// Optional<BigDecimal[]> optAvgConsumptionArray = billEntities.stream()
		// .map(FuelBillEntity::getAvgConsumption).map(bd -> new BigDecimal[] {
		// bd, BigDecimal.ONE
		// })
		// .reduce((a, b) -> new BigDecimal[] {
		// a[0].add(b[0]), a[1].add(BigDecimal.ONE)
		// });
		// if (optAvgConsumptionArray.isPresent())
		// {
		// BigDecimal[] avgConsumptionArray = optAvgConsumptionArray.get();
		// avgConsumption = avgConsumptionArray[0].divide(avgConsumptionArray[1],
		// RoundingMode.HALF_UP);
		// }

		BigDecimal avgConsumption = BigDecimal.ZERO;
		Optional<BigDecimal[]> optAvgConsumptionArray = billEntities.stream()
				.map(BillEntity::getFuelBill)
				.filter(Objects::nonNull)
				.map(FuelBillEntity::getAvgConsumption).map(bd -> new BigDecimal[] {
						bd, BigDecimal.ONE
				})
				.reduce((a, b) -> new BigDecimal[] {
						a[0].add(b[0]), a[1].add(BigDecimal.ONE)
				});
		if (optAvgConsumptionArray.isPresent())
		{
			BigDecimal[] avgConsumptionArray = optAvgConsumptionArray.get();
			avgConsumption = avgConsumptionArray[0].divide(avgConsumptionArray[1],
					RoundingMode.HALF_UP);
		}

		BigInteger totalRefuels = BigInteger
				.valueOf(billEntities.stream().filter(x -> x.getType().equals(BillType.FUEL)).count());

		return new CarOverview(totalRefuels, totalCost, avgConsumption);
	}
}
