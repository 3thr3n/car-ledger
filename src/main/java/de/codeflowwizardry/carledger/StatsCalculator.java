package de.codeflowwizardry.carledger;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import de.codeflowwizardry.carledger.data.*;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository.MonthFuel;
import de.codeflowwizardry.carledger.data.repository.BillRepository.MonthTotal;
import de.codeflowwizardry.carledger.data.repository.RecurringBillPaymentRepository;
import de.codeflowwizardry.carledger.rest.car.stats.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StatsCalculator
{
	public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	/** guards against absurd ranges, 100 years */
	static final int MAX_MONTHS = 1200;

	private final BillRepository repository;
	private final RecurringBillPaymentRepository recurringBillPaymentRepository;

	@Inject
	public StatsCalculator(BillRepository billRepository,
			RecurringBillPaymentRepository recurringBillPaymentRepository)
	{
		this.repository = billRepository;
		this.recurringBillPaymentRepository = recurringBillPaymentRepository;
	}

	public DashboardStats calculateStats(Long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
	{
		List<BillEntity> billEntities = repository.getBills(carId, username, from, to);

		List<FuelBillEntity> fuelBillList = billEntities.stream()
				.map(BillEntity::getFuelBill)
				.filter(Objects::nonNull)
				.toList();

		List<MaintenanceBillEntity> maintenanceBillList = billEntities.stream()
				.map(BillEntity::getMaintenanceBill)
				.filter(Objects::nonNull)
				.toList();

		List<MiscellaneousBillEntity> miscellaneousBillList = billEntities.stream()
				.map(BillEntity::getMiscellaneousBill)
				.filter(Objects::nonNull)
				.toList();

		BigDecimal recurringTotal = recurringBillPaymentRepository.sumAmount(carId, username, from, to);

		return new DashboardStats(
				calculateTotal(fuelBillList, maintenanceBillList, miscellaneousBillList, recurringTotal),
				calculateAverage(fuelBillList, maintenanceBillList, miscellaneousBillList, recurringTotal),
				calculateHighLow(fuelBillList, maintenanceBillList));
	}

	/**
	 * Costs and fuel per month, the sums are calculated by the database. Every
	 * month of the range is included, also the ones without bills. Without a
	 * from/to, the range starts/ends with the first/last month with bills.
	 */
	public List<MonthStats> calculateMonthly(Long carId, String username, Optional<LocalDate> from,
			Optional<LocalDate> to)
	{
		Map<YearMonth, Map<BillType, BigDecimal>> totals = new HashMap<>();
		for (MonthTotal row : repository.sumPerMonth(carId, username, from, to))
		{
			totals.computeIfAbsent(row.month(), month -> new EnumMap<>(BillType.class)).put(row.type(), row.total());
		}
		Map<YearMonth, MonthFuel> fuel = new HashMap<>();
		repository.sumFuelPerMonth(carId, username, from, to).forEach(row -> fuel.put(row.month(), row));
		Map<YearMonth, BigDecimal> recurring = recurringBillPaymentRepository.sumAmountPerMonth(carId, username,
				from, to);

		SortedSet<YearMonth> withData = new TreeSet<>(totals.keySet());
		withData.addAll(recurring.keySet());

		Optional<YearMonth> first = from.map(YearMonth::from)
				.or(() -> withData.isEmpty() ? Optional.empty() : Optional.of(withData.first()));
		Optional<YearMonth> last = to.map(YearMonth::from)
				.or(() -> withData.isEmpty() ? Optional.empty() : Optional.of(withData.last()));
		if (first.isEmpty() || last.isEmpty() || first.get().isAfter(last.get()))
		{
			return List.of();
		}

		YearMonth start = first.get();
		if (ChronoUnit.MONTHS.between(start, last.get()) >= MAX_MONTHS)
		{
			start = last.get().minusMonths(MAX_MONTHS - 1);
		}

		List<MonthStats> months = new ArrayList<>();
		for (YearMonth month = start; !month.isAfter(last.get()); month = month.plusMonths(1))
		{
			months.add(calculateMonth(month, totals.getOrDefault(month, Map.of()), fuel.get(month),
					recurring.getOrDefault(month, ZERO)));
		}
		return months;
	}

	private static MonthStats calculateMonth(YearMonth month, Map<BillType, BigDecimal> totals, MonthFuel fuel,
			BigDecimal recurring)
	{
		BigDecimal fuelTotal = totals.getOrDefault(BillType.FUEL, ZERO);
		BigDecimal maintenance = totals.getOrDefault(BillType.MAINTENANCE, ZERO);
		BigDecimal miscellaneous = totals.getOrDefault(BillType.MISCELLANEOUS, ZERO);
		BigDecimal total = fuelTotal.add(maintenance).add(miscellaneous).add(recurring);

		BigDecimal unit = fuel == null ? ZERO : fuel.unit();
		BigDecimal distance = fuel == null ? ZERO : fuel.distance();

		BigDecimal pricePerUnit = Utils.valueWasSet(unit)
				? fuel.pricedUnit().divide(unit, 1, RoundingMode.HALF_UP)
				: null;
		boolean hasDistance = Utils.valueWasSet(distance);
		BigDecimal consumption = hasDistance
				? fuel.unitWithDistance().multiply(ONE_HUNDRED).divide(distance, 2, RoundingMode.HALF_UP)
				: null;
		BigDecimal costPerKm = hasDistance
				? total.multiply(ONE_HUNDRED).divide(distance, 2, RoundingMode.HALF_UP)
				: null;

		return new MonthStats(month.toString(), money(fuelTotal), money(maintenance), money(miscellaneous),
				money(recurring), money(total), money(distance), money(unit), pricePerUnit, consumption, costPerKm);
	}

	private static BigDecimal money(BigDecimal value)
	{
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * Calculates the total of:
	 * <li>Units (liters/gallons)
	 * <li>Distance
	 * <li>Fuel cost
	 * <li>Maintenance cost
	 * <li>Total cost (fuel + maintenance)
	 * <li>Number of fuel bills
	 * <li>Number of maintenance bills
	 */
	private TotalStats calculateTotal(List<FuelBillEntity> fuelBillList,
			List<MaintenanceBillEntity> maintenanceBillList,
			List<MiscellaneousBillEntity> miscellaneousBillList,
			BigDecimal recurringTotal)
	{
		BigDecimal unit = calculateTotalUnit(fuelBillList);
		BigDecimal trackedDistance = calculateTotalDistance(fuelBillList);
		BigDecimal fuelTotal = calculateTotal(fuelBillList);
		BigDecimal maintenanceTotal = calculateTotal(maintenanceBillList);
		BigDecimal miscellaneousTotal = calculateTotal(miscellaneousBillList);
		Integer fuelBills = fuelBillList.size();
		Integer maintenanceBills = maintenanceBillList.size();
		Integer miscellaneousBills = miscellaneousBillList.size();

		BigDecimal total = ZERO
				.add(fuelTotal)
				.add(maintenanceTotal)
				.add(miscellaneousTotal)
				.add(recurringTotal);

		return new TotalStats(unit, trackedDistance, fuelTotal, maintenanceTotal, miscellaneousTotal, total, fuelBills,
				maintenanceBills, miscellaneousBills);
	}

	private static BigDecimal calculateTotalUnit(List<FuelBillEntity> entities)
	{
		Stream<BigDecimal> totalUnit = entities.stream().map(FuelBillEntity::getUnit);
		return handleReduceTotalResult(totalUnit);
	}

	private static BigDecimal calculateTotalDistance(List<FuelBillEntity> entities)
	{
		Stream<BigDecimal> totalUnit = entities.stream().map(FuelBillEntity::getDistance);
		return handleReduceTotalResult(totalUnit);
	}

	private static BigDecimal calculateTotal(List<? extends AbstractBillEntity> entities)
	{
		Stream<BigDecimal> totalCost = entities.stream().map(x -> x.getBill().getTotal());
		return handleReduceTotalResult(totalCost);
	}

	private static BigDecimal handleReduceTotalResult(Stream<BigDecimal> decimalStream)
	{
		return decimalStream.filter(Objects::nonNull).reduce(BigDecimal::add).orElse(ZERO);
	}

	/**
	 * Calculates the average of:
	 * <li>Price per unit
	 * <li>Distance per fill-up
	 * <li>Cost per km
	 * <li>Fuel consumption (L/100km)
	 * <li>Maintenance cost per bill
	 * <li>Cost per km (total, including maintenance)
	 */
	private AverageStats calculateAverage(List<FuelBillEntity> fuelBillList,
			List<MaintenanceBillEntity> maintenanceBillList, List<MiscellaneousBillEntity> miscellaneousBillList,
			BigDecimal recurringTotal)
	{
		// Price per unit
		BigDecimal pricePerUnit = calculateAveragePricePerUnit(fuelBillList);
		// Distance per fill-up
		BigDecimal distance = calculateAverageDistance(fuelBillList);
		// Cost per km
		BigDecimal fuelCostPerKm = calculateAverageFuelCostPerKm(fuelBillList);
		// Fuel consumption (L/100km)
		BigDecimal consumption = calculateAverageConsumption(fuelBillList);
		// Maintenance cost per bill
		BigDecimal averageMaintenancePrice = calculateAverageMaintenance(maintenanceBillList);
		// Cost per km (total, including maintenance)
		BigDecimal costPerKm = calculateAverageCostPerKm(fuelBillList, maintenanceBillList, miscellaneousBillList,
				recurringTotal);

		return new AverageStats(pricePerUnit, distance, fuelCostPerKm, consumption, averageMaintenancePrice, costPerKm);
	}

	private static BigDecimal calculateAveragePricePerUnit(List<FuelBillEntity> entities)
	{
		return handleReduceAverageResult(entities, FuelBillEntity::getPricePerUnit);
	}

	private static BigDecimal calculateAverageDistance(List<FuelBillEntity> entities)
	{
		return handleReduceAverageResult(entities, FuelBillEntity::getDistance);
	}

	private static BigDecimal calculateAverageFuelCostPerKm(List<FuelBillEntity> entities)
	{
		return handleReduceAverageResult(entities, FuelBillEntity::getCostPerKm);
	}

	private static BigDecimal calculateAverageConsumption(List<FuelBillEntity> entities)
	{
		return handleReduceAverageResult(entities, FuelBillEntity::getAvgConsumption);
	}

	private static BigDecimal calculateAverageMaintenance(List<MaintenanceBillEntity> entities)
	{
		return handleReduceAverageResult(entities, x -> x.getBill().getTotal());
	}

	private static BigDecimal calculateAverageCostPerKm(List<FuelBillEntity> fuelBillEntities,
			List<MaintenanceBillEntity> maintenanceBillList, List<MiscellaneousBillEntity> miscellaneousBillList,
			BigDecimal recurring)
	{
		BigDecimal maintenance = calculateTotal(maintenanceBillList);
		BigDecimal fuel = calculateTotal(fuelBillEntities);
		BigDecimal miscellaneous = calculateTotal(miscellaneousBillList);

		BigDecimal distance = calculateTotalDistance(fuelBillEntities);

		BigDecimal total = ZERO.add(maintenance).add(fuel).add(miscellaneous).add(recurring);

		if (!Utils.valueWasSet(total) || !Utils.valueWasSet(distance))
		{
			return BigDecimal.ZERO;
		}

		return total.divide(distance, 4, RoundingMode.HALF_UP).multiply(ONE_HUNDRED);
	}

	private static <T extends AbstractBillEntity> BigDecimal handleReduceAverageResult(List<T> billEntities,
			Function<T, BigDecimal> bigDecimalMapFunction)
	{
		Optional<BigDecimal[]> optionalBigDecimal = billEntities.stream()
				.map(bigDecimalMapFunction)
				// unset values (e.g. no distance tracked) must not drag the average down
				.filter(Utils::valueWasSet)
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

	private HiLoStats calculateHighLow(List<FuelBillEntity> fuelBillList,
			List<MaintenanceBillEntity> maintenanceBillList)
	{
		HiLo pricePerUnit = calculateHiLoPricePerUnit(fuelBillList);
		HiLo distance = calculateHiLoDistance(fuelBillList);
		HiLo fuelCostPerKm = calculateHiLoFuelCostPerKm(fuelBillList);
		HiLo consumption = calculateHiLoCalculated(fuelBillList);
		HiLo fuelCost = calculateHiLoFuelPrice(fuelBillList);
		HiLo maintenanceCost = calculateHiLoMaintenancePrice(maintenanceBillList);

		return new HiLoStats(pricePerUnit, distance, fuelCostPerKm, consumption, fuelCost, maintenanceCost);
	}

	private static HiLo calculateHiLoDistance(List<FuelBillEntity> billEntities)
	{
		return calculateHiLo(billEntities, FuelBillEntity::getDistance);
	}

	private static HiLo calculateHiLoFuelCostPerKm(List<FuelBillEntity> billEntities)
	{
		return calculateHiLo(billEntities, FuelBillEntity::getCostPerKm);
	}

	private static HiLo calculateHiLoPricePerUnit(List<FuelBillEntity> billEntities)
	{
		return calculateHiLo(billEntities, FuelBillEntity::getPricePerUnit, 1);
	}

	private static HiLo calculateHiLoCalculated(List<FuelBillEntity> billEntities)
	{
		return calculateHiLo(billEntities, FuelBillEntity::getAvgConsumption);
	}

	private static HiLo calculateHiLoFuelPrice(List<FuelBillEntity> billEntities)
	{
		return calculateHiLo(billEntities, x -> x.getBill().getTotal());
	}

	private static HiLo calculateHiLoMaintenancePrice(List<MaintenanceBillEntity> billEntities)
	{
		return calculateHiLo(billEntities, x -> x.getBill().getTotal());
	}

	private static <T extends AbstractBillEntity> HiLo calculateHiLo(List<T> billEntities,
			Function<T, BigDecimal> bigDecimalFunction, int scale)
	{
		BigDecimal min = billEntities.stream()
				.map(bigDecimalFunction)
				.filter(bigDecimal -> bigDecimal != null && bigDecimal.signum() != 0)
				.min(Comparator.naturalOrder())
				.orElse(ZERO);

		BigDecimal max = billEntities.stream()
				.map(bigDecimalFunction)
				.filter(bigDecimal -> bigDecimal != null && bigDecimal.signum() != 0)
				.max(Comparator.naturalOrder())
				.orElse(ZERO);

		return new HiLo(min, max, scale);
	}

	private static <T extends AbstractBillEntity> HiLo calculateHiLo(List<T> billEntities,
			Function<T, BigDecimal> bigDecimalFunction)
	{
		return calculateHiLo(billEntities, bigDecimalFunction, 2);
	}
}
