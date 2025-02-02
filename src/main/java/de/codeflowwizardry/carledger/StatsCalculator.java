package de.codeflowwizardry.carledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import de.codeflowwizardry.carledger.data.Bill;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.rest.records.stats.AverageStats;
import de.codeflowwizardry.carledger.rest.records.stats.HiLo;
import de.codeflowwizardry.carledger.rest.records.stats.HiLoStats;
import de.codeflowwizardry.carledger.rest.records.stats.MinimalStats;
import de.codeflowwizardry.carledger.rest.records.stats.TotalStats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StatsCalculator
{
	public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	private final BillRepository billRepository;

	@Inject
	public StatsCalculator(BillRepository billRepository)
	{
		this.billRepository = billRepository;
	}

	public TotalStats calculateTotal(Long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
	{
		List<Bill> bills = billRepository.getBills(carId, username, from, to);

		BigDecimal unit = calculateTotalUnit(bills);
		BigDecimal distance = calculateTotalDistance(bills);
		BigDecimal calculatedPrice = calculateTotalCalculatedPrice(bills);

		return new TotalStats(unit, distance, calculatedPrice);
	}

	private BigDecimal calculateTotalUnit(List<Bill> bills)
	{
		Stream<BigDecimal> totalUnit = bills.stream().map(Bill::getUnit);
		return handleReduceTotalResult(totalUnit);
	}

	private BigDecimal calculateTotalDistance(List<Bill> bills)
	{
		Stream<BigDecimal> totalUnit = bills.stream().map(Bill::getDistance);
		return handleReduceTotalResult(totalUnit);
	}

	private BigDecimal calculateTotalCalculatedPrice(List<Bill> bills)
	{
		Stream<BigDecimal> totalCost = bills.stream()
				.map(bill -> bill.getCalculatedPrice(BigDecimal.valueOf(1.19)));
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
		List<Bill> bills = billRepository.getBills(carId, username, from, to);

		BigDecimal averagePricePerUnit = calculateAveragePricePerUnit(bills);
		BigDecimal averageDistance = calculateAverageDistance(bills);
		BigDecimal averageCalculated = calculateAverageCalculated(bills);
		BigDecimal averageCalculatedPrice = calculateAverageCalculatedPrice(bills);

		return new AverageStats(averagePricePerUnit, averageDistance, averageCalculated, averageCalculatedPrice);
	}

	private static BigDecimal calculateAverageDistance(List<Bill> bills)
	{
		return handleReduceAverageResult(bills, Bill::getDistance);
	}

	private static BigDecimal calculateAveragePricePerUnit(List<Bill> bills)
	{
		return handleReduceAverageResult(bills, Bill::getPricePerUnit);
	}

	private static BigDecimal calculateAverageCalculated(List<Bill> bills)
	{
		return handleReduceAverageResult(bills,
				bill -> bill.getUnit().divide(bill.getDistance(), 6, RoundingMode.HALF_UP)
						.multiply(ONE_HUNDRED));
	}

	private static BigDecimal calculateAverageCalculatedPrice(List<Bill> bills)
	{
		return handleReduceAverageResult(bills,
				bill -> bill.getCalculatedPrice(BigDecimal.valueOf(1.19)));
	}

	private static BigDecimal handleReduceAverageResult(List<Bill> bills,
			Function<Bill, BigDecimal> bigDecimalMapFunction)
	{
		Optional<BigDecimal[]> optionalBigDecimal = bills.stream()
				.filter(Bill::isDistanceSet)
				.map(bigDecimalMapFunction)
				.map(bd -> new BigDecimal[] {
						bd, BigDecimal.ONE
				})
				.reduce((a, b) -> new BigDecimal[] {
						a[0].add(b[0]), a[1].add(BigDecimal.ONE)
				});

		if (optionalBigDecimal.isEmpty())
		{
			throw new IllegalStateException("Should not be possible");
		}
		BigDecimal[] totalWithCount = optionalBigDecimal.get();
		return totalWithCount[0].divide(totalWithCount[1], RoundingMode.HALF_UP);
	}

	public HiLoStats calculateHighLow(Long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
	{
		List<Bill> bills = billRepository.getBills(carId, username, from, to);

		HiLo hiLoDistance = calculateHiLoDistance(bills);
		HiLo hiLoUnit = calculateHiLoUnit(bills);
		HiLo hiLoPricePerUnit = calculateHiLoPricePerUnit(bills);
		HiLo hiLoCalculated = calculateHiLoCalculated(bills);
		HiLo hiLoCalculatedPrice = calculateHiLoCalculatedPrice(bills);

		return new HiLoStats(hiLoCalculatedPrice, hiLoCalculated, hiLoDistance, hiLoUnit, hiLoPricePerUnit);
	}

	private static HiLo calculateHiLoDistance(List<Bill> bills)
	{
		return calculateHiLo(bills, Bill::getDistance);
	}

	private static HiLo calculateHiLoUnit(List<Bill> bills)
	{
		return calculateHiLo(bills, Bill::getUnit);
	}

	private static HiLo calculateHiLoPricePerUnit(List<Bill> bills)
	{
		return calculateHiLo(bills, Bill::getPricePerUnit, 1);
	}

	private static HiLo calculateHiLoCalculated(List<Bill> bills)
	{
		return calculateHiLo(bills,
				bill -> bill.getUnit().divide(bill.getDistance(), 6, RoundingMode.HALF_UP)
						.multiply(ONE_HUNDRED));
	}

	private static HiLo calculateHiLoCalculatedPrice(List<Bill> bills)
	{
		return calculateHiLo(bills,
				bill -> bill.getCalculatedPrice(BigDecimal.valueOf(1.19)));
	}

	private static HiLo calculateHiLo(List<Bill> bills, Function<Bill, BigDecimal> bigDecimalFunction, int scale)
	{
		BigDecimal min = bills.stream()
				.filter(Bill::isDistanceSet)
				.map(bigDecimalFunction)
				.min(Comparator.naturalOrder())
				.orElse(BigDecimal.ZERO);

		BigDecimal max = bills.stream()
				.filter(Bill::isDistanceSet)
				.map(bigDecimalFunction)
				.max(Comparator.naturalOrder())
				.orElse(BigDecimal.ZERO);

		return new HiLo(min, max, scale);
	}

	private static HiLo calculateHiLo(List<Bill> bills, Function<Bill, BigDecimal> bigDecimalFunction)
	{
		return calculateHiLo(bills, bigDecimalFunction, 2);
	}

	public MinimalStats getMinimalStats(Long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
	{
		List<Bill> bills = billRepository.getBills(carId, username, from, to);

		BigDecimal calculatedPrice = calculateTotalCalculatedPrice(bills);
		BigDecimal averageCalculated = calculateAverageCalculated(bills);
		HiLo hiLoCalculated = calculateHiLoCalculated(bills);
		BigDecimal averageDistance = calculateAverageDistance(bills);

		return new MinimalStats(calculatedPrice, averageCalculated, hiLoCalculated, averageDistance);
	}
}
