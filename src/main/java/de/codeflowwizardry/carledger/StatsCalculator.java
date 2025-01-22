package de.codeflowwizardry.carledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import de.codeflowwizardry.carledger.data.Bill;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.rest.records.stats.AverageStats;
import de.codeflowwizardry.carledger.rest.records.stats.TotalStats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StatsCalculator
{
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

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
				.map(bill -> bill.getUnit().multiply(bill.getPricePerUnit()).divide(ONE_HUNDRED, RoundingMode.HALF_UP));
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

		BigDecimal averagePricePerUnit = calculateAveragePpu(bills);
		BigDecimal averageDistance = calculateAverageDistance(bills);
		BigDecimal averageCalculated = calculateAverageCalculated(bills);
		BigDecimal averageCalculatedPrice = calculateAverageCalculatedPrice(bills);

		return new AverageStats(averagePricePerUnit, averageDistance, averageCalculated, averageCalculatedPrice);
	}

	private BigDecimal calculateAverageDistance(List<Bill> bills)
	{
		Stream<BigDecimal> bigDecimalStream = bills.stream()
				.map(Bill::getDistance);
		return handleReduceAverageResult(bigDecimalStream);
	}

	private BigDecimal calculateAveragePpu(List<Bill> bills)
	{
		Stream<BigDecimal> bigDecimalStream = bills.stream()
				.map(Bill::getPricePerUnit);
		return handleReduceAverageResult(bigDecimalStream);
	}

	private BigDecimal calculateAverageCalculated(List<Bill> bills)
	{
		Stream<BigDecimal> bigDecimalStream = bills.stream()
				.map(bill -> bill.getUnit().divide(bill.getDistance(), RoundingMode.HALF_UP).multiply(ONE_HUNDRED));
		return handleReduceAverageResult(bigDecimalStream);
	}

	private BigDecimal calculateAverageCalculatedPrice(List<Bill> bills)
	{
		Stream<BigDecimal> bigDecimalStream = bills.stream()
				.map(bill -> bill.getUnit().multiply(bill.getPricePerUnit()).divide(ONE_HUNDRED, RoundingMode.HALF_UP));
		return handleReduceAverageResult(bigDecimalStream);
	}

	private static BigDecimal handleReduceAverageResult(Stream<BigDecimal> bigDecimalStream)
	{
		Optional<BigDecimal[]> optionalBigDecimal = bigDecimalStream
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

}
