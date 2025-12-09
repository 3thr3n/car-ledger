package de.codeflowwizardry.carledger.rest.records;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.FuelBillEntity;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static de.codeflowwizardry.carledger.data.BillEntity.GERMAN_UST;

public record CarOverview(BigInteger totalRefuels, BigDecimal totalCost, BigDecimal avgConsumption)
{
	public static CarOverview convert(CarEntity carEntity)
	{
		List<FuelBillEntity> billEntities = carEntity.getBills()
                .stream()
                .filter(f -> f instanceof FuelBillEntity)
				.map(f -> (FuelBillEntity) f)
                .toList();

		BigDecimal totalCost = billEntities.stream().map(BillEntity::getTotal).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		BigDecimal avgConsumption = BigDecimal.ZERO;
		Optional<BigDecimal[]> optAvgConsumptionArray = billEntities.stream()
				.map(FuelBillEntity::getAvgConsumption).map(bd -> new BigDecimal[] {
						bd, BigDecimal.ONE
				})
				.reduce((a, b) -> new BigDecimal[] {
						a[0].add(b[0]), a[1].add(BigDecimal.ONE)
				});
		if (optAvgConsumptionArray.isPresent())
		{
			BigDecimal[] avgConsumptionArray = optAvgConsumptionArray.get();
			avgConsumption = avgConsumptionArray[0].divide(avgConsumptionArray[1], RoundingMode.HALF_UP);
		}

		return new CarOverview(BigInteger.valueOf(billEntities.size()), totalCost, avgConsumption);
	}
}
