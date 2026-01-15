package de.codeflowwizardry.carledger;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.data.repository.FuelBillRepository;
import de.codeflowwizardry.carledger.rest.records.stats.AverageStats;
import de.codeflowwizardry.carledger.rest.records.stats.HiLo;
import de.codeflowwizardry.carledger.rest.records.stats.HiLoStats;
import de.codeflowwizardry.carledger.rest.records.stats.TotalStats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StatsCalculator
{
	public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	private final FuelBillRepository billRepository;

	@Inject
	public StatsCalculator(FuelBillRepository billRepository)
	{
		this.billRepository = billRepository;
	}

	public TotalStats calculateTotal(Long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
	{
		List<FuelBillEntity> billEntities = billRepository.getBills(carId, username, from, to);

		if (billEntities.isEmpty())
		{
			return new TotalStats(ZERO, ZERO, ZERO);
		}

		BigDecimal unit = calculateTotalUnit(billEntities);
		BigDecimal distance = calculateTotalDistance(billEntities);
		BigDecimal calculatedPrice = calculateTotalCalculatedPrice(billEntities);

		return new TotalStats(unit, distance, calculatedPrice);
	}

	private BigDecimal calculateTotalUnit(List<FuelBillEntity> billEntities)
	{
		Stream<BigDecimal> totalUnit = billEntities.stream().map(FuelBillEntity::getUnit);
		return handleReduceTotalResult(totalUnit);
	}

	private BigDecimal calculateTotalDistance(List<FuelBillEntity> billEntities)
	{
		Stream<BigDecimal> totalUnit = billEntities.stream().map(FuelBillEntity::getDistance);
		return handleReduceTotalResult(totalUnit);
	}

	private BigDecimal calculateTotalCalculatedPrice(List<FuelBillEntity> billEntities)
	{
		Stream<BigDecimal> totalCost = billEntities.stream()
				.map(x -> x.getBill().getTotal());
		return handleReduceTotalResult(totalCost);
	}

	private static BigDecimal handleReduceTotalResult(Stream<BigDecimal> decimalStream)
	{
		Optional<BigDecimal> optionalBigDecimal = decimalStream.reduce(BigDecimal::add);

		if (optionalBigDecimal.isEmpty())
		{
			throw new IllegalStateException("Should not be possible");
		}
		return optionalBigDecimal.get();
	}

	public AverageStats calculateAverage(Long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
	{
		List<FuelBillEntity> billEntities = billRepository.getBills(carId, username, from, to);

		BigDecimal averagePricePerUnit = calculateAveragePricePerUnit(billEntities);
		BigDecimal averageDistance = calculateAverageDistance(billEntities);
		BigDecimal averageCalculated = calculateAverageCalculated(billEntities);
		BigDecimal averageCalculatedPrice = calculateAverageCalculatedPrice(billEntities);

		return new AverageStats(averagePricePerUnit, averageDistance, averageCalculated, averageCalculatedPrice);
	}

	private static BigDecimal calculateAverageDistance(List<FuelBillEntity> billEntities)
	{
		return handleReduceAverageResult(billEntities, FuelBillEntity::getDistance);
	}

	private static BigDecimal calculateAveragePricePerUnit(List<FuelBillEntity> billEntities)
	{
		return handleReduceAverageResult(billEntities, FuelBillEntity::getPricePerUnit);
	}

	private static BigDecimal calculateAverageCalculated(List<FuelBillEntity> billEntities)
	{
		return handleReduceAverageResult(billEntities, FuelBillEntity::getAvgConsumption);
	}

	private static BigDecimal calculateAverageCalculatedPrice(List<FuelBillEntity> billEntities)
	{
		return handleReduceAverageResult(billEntities, x -> x.getBill().getTotal());
	}

	private static BigDecimal handleReduceAverageResult(List<FuelBillEntity> billEntities,
			Function<FuelBillEntity, BigDecimal> bigDecimalMapFunction)
	{
		Optional<BigDecimal[]> optionalBigDecimal = billEntities.stream()
				.map(bigDecimalMapFunction)
				.map(bd -> new BigDecimal[] {
						bd, BigDecimal.ONE
				})
				.reduce((a, b) -> new BigDecimal[] {
						a[0].add(b[0]), a[1].add(BigDecimal.ONE)
				});

		if (optionalBigDecimal.isEmpty())
		{
			return ZERO;
		}
		BigDecimal[] totalWithCount = optionalBigDecimal.get();
		return totalWithCount[0].divide(totalWithCount[1], RoundingMode.HALF_UP);
	}

	public HiLoStats calculateHighLow(Long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
	{
		List<FuelBillEntity> billEntities = billRepository.getBills(carId, username, from, to);

		HiLo hiLoDistance = calculateHiLoDistance(billEntities);
		HiLo hiLoUnit = calculateHiLoUnit(billEntities);
		HiLo hiLoPricePerUnit = calculateHiLoPricePerUnit(billEntities);
		HiLo hiLoCalculated = calculateHiLoCalculated(billEntities);
		HiLo hiLoCalculatedPrice = calculateHiLoCalculatedPrice(billEntities);

		return new HiLoStats(hiLoCalculatedPrice, hiLoCalculated, hiLoDistance, hiLoUnit, hiLoPricePerUnit);
	}

	private static HiLo calculateHiLoDistance(List<FuelBillEntity> billEntities)
	{
		return calculateHiLo(billEntities, FuelBillEntity::getDistance);
	}

	private static HiLo calculateHiLoUnit(List<FuelBillEntity> billEntities)
	{
		return calculateHiLo(billEntities, FuelBillEntity::getUnit);
	}

	private static HiLo calculateHiLoPricePerUnit(List<FuelBillEntity> billEntities)
	{
		return calculateHiLo(billEntities, FuelBillEntity::getPricePerUnit, 1);
	}

	private static HiLo calculateHiLoCalculated(List<FuelBillEntity> billEntities)
	{
		return calculateHiLo(billEntities,
				bill -> bill.getUnit().divide(bill.getDistance(), 6, RoundingMode.HALF_UP)
						.multiply(ONE_HUNDRED));
	}

	private static HiLo calculateHiLoCalculatedPrice(List<FuelBillEntity> billEntities)
	{
		return calculateHiLo(billEntities, x -> x.getBill().getTotal());
	}

	private static HiLo calculateHiLo(List<FuelBillEntity> billEntities,
			Function<FuelBillEntity, BigDecimal> bigDecimalFunction, int scale)
	{
		BigDecimal min = billEntities.stream()
				.filter(FuelBillEntity::isDistanceSet)
				.map(bigDecimalFunction)
				.min(Comparator.naturalOrder())
				.orElse(ZERO);

		BigDecimal max = billEntities.stream()
				.filter(FuelBillEntity::isDistanceSet)
				.map(bigDecimalFunction)
				.max(Comparator.naturalOrder())
				.orElse(ZERO);

		return new HiLo(min, max, scale);
	}

	private static HiLo calculateHiLo(List<FuelBillEntity> billEntities,
			Function<FuelBillEntity, BigDecimal> bigDecimalFunction)
	{
		return calculateHiLo(billEntities, bigDecimalFunction, 2);
	}
}
