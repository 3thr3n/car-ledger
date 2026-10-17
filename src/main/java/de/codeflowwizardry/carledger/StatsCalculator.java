package de.codeflowwizardry.carledger;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.codeflowwizardry.carledger.data.*;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.data.repository.BillRepository.Energy;
import de.codeflowwizardry.carledger.data.repository.BillRepository.MonthEnergy;
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

		Bills bills = new Bills(
				parts(billEntities, BillEntity::getFuelBill),
				parts(billEntities, BillEntity::getChargingBill),
				parts(billEntities, BillEntity::getMaintenanceBill),
				parts(billEntities, BillEntity::getMiscellaneousBill),
				recurringBillPaymentRepository.sumAmount(carId, username, from, to));

		return new DashboardStats(calculateTotal(bills), calculateAverage(bills), calculateHighLow(bills));
	}

	private static <T> List<T> parts(List<BillEntity> bills, Function<BillEntity, T> part)
	{
		return bills.stream().map(part).filter(Objects::nonNull).toList();
	}

	/**
	 * The bills of the period by their type
	 *
	 * @param recurring
	 *            sum of the payments of the recurring costs
	 */
	private record Bills(List<FuelBillEntity> fuel, List<ChargingBillEntity> charging,
			List<MaintenanceBillEntity> maintenance, List<MiscellaneousBillEntity> miscellaneous,
			BigDecimal recurring)
	{
		BigDecimal total()
		{
			return sumTotal(fuel).add(sumTotal(charging)).add(sumTotal(maintenance)).add(sumTotal(miscellaneous))
					.add(recurring);
		}
	}

	/**
	 * Costs, liters and kWh per month, the sums are calculated by the database.
	 * Every month of the range is included, also the ones without bills. Without a
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

		Map<YearMonth, List<MonthEnergy>> fuel = byMonth(
				repository.sumEnergyPerMonth(carId, username, from, to, Energy.FUEL));
		Map<YearMonth, List<MonthEnergy>> charging = byMonth(
				repository.sumEnergyPerMonth(carId, username, from, to, Energy.CHARGING));
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
			months.add(calculateMonth(month, totals.getOrDefault(month, Map.of()),
					fuel.getOrDefault(month, List.of()), charging.getOrDefault(month, List.of()),
					recurring.getOrDefault(month, ZERO)));
		}
		return months;
	}

	private static Map<YearMonth, List<MonthEnergy>> byMonth(List<MonthEnergy> rows)
	{
		return rows.stream().collect(Collectors.groupingBy(MonthEnergy::month));
	}

	private static MonthStats calculateMonth(YearMonth month, Map<BillType, BigDecimal> totals,
			List<MonthEnergy> fuelRows, List<MonthEnergy> chargingRows, BigDecimal recurring)
	{
		BigDecimal fuelTotal = totals.getOrDefault(BillType.FUEL, ZERO);
		BigDecimal chargingTotal = totals.getOrDefault(BillType.CHARGING, ZERO);
		BigDecimal maintenance = totals.getOrDefault(BillType.MAINTENANCE, ZERO);
		BigDecimal miscellaneous = totals.getOrDefault(BillType.MISCELLANEOUS, ZERO);
		BigDecimal total = fuelTotal.add(chargingTotal).add(maintenance).add(miscellaneous).add(recurring);

		EnergyMonth fuel = EnergyMonth.of(fuelRows);
		EnergyMonth charging = EnergyMonth.of(chargingRows);
		BigDecimal distance = trackedDistance(
				fuelRows.stream().collect(Collectors.toMap(MonthEnergy::carId, MonthEnergy::distance)),
				chargingRows.stream().collect(Collectors.toMap(MonthEnergy::carId, MonthEnergy::distance)));
		BigDecimal costPerKm = Utils.valueWasSet(distance)
				? total.multiply(ONE_HUNDRED).divide(distance, 2, RoundingMode.HALF_UP)
				: null;

		return new MonthStats(month.toString(), money(fuelTotal), money(chargingTotal), money(maintenance),
				money(miscellaneous), money(recurring), money(total), money(distance), money(fuel.unit()),
				money(charging.unit()), fuel.pricePerUnit(), charging.pricePerUnit(), fuel.consumption(),
				charging.consumption(), costPerKm);
	}

	/**
	 * The refuels or the charges of one month, summed over the cars
	 */
	private record EnergyMonth(BigDecimal unit, BigDecimal distance, BigDecimal unitWithDistance,
			BigDecimal pricedUnit)
	{
		static EnergyMonth of(List<MonthEnergy> rows)
		{
			return new EnergyMonth(
					sum(rows.stream().map(MonthEnergy::unit)),
					sum(rows.stream().map(MonthEnergy::distance)),
					sum(rows.stream().map(MonthEnergy::unitWithDistance)),
					sum(rows.stream().map(MonthEnergy::pricedUnit)));
		}

		/**
		 * @return weighted by the liters or kWh, <code>null</code> without any
		 */
		BigDecimal pricePerUnit()
		{
			return Utils.valueWasSet(unit) ? pricedUnit.divide(unit, 1, RoundingMode.HALF_UP) : null;
		}

		/**
		 * @return per 100 km of the refuels or charges with a distance,
		 *         <code>null</code> without any
		 */
		BigDecimal consumption()
		{
			return Utils.valueWasSet(distance)
					? unitWithDistance.multiply(ONE_HUNDRED).divide(distance, 2, RoundingMode.HALF_UP)
					: null;
		}
	}

	private static BigDecimal money(BigDecimal value)
	{
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * The distance driven: per car the larger one of the refuels and the charges.
	 * Both are the km since the last refuel or charge, a plug-in hybrid would count
	 * its km twice with their sum.
	 *
	 * @param fuel
	 *            distance of the refuels per car id
	 * @param charging
	 *            distance of the charges per car id
	 */
	static BigDecimal trackedDistance(Map<Long, BigDecimal> fuel, Map<Long, BigDecimal> charging)
	{
		Set<Long> cars = new HashSet<>(fuel.keySet());
		cars.addAll(charging.keySet());
		return cars.stream()
				.map(car -> fuel.getOrDefault(car, ZERO).max(charging.getOrDefault(car, ZERO)))
				.reduce(ZERO, BigDecimal::add);
	}

	private static BigDecimal trackedDistance(Bills bills)
	{
		return trackedDistance(distancePerCar(bills.fuel()), distancePerCar(bills.charging()));
	}

	private static Map<Long, BigDecimal> distancePerCar(List<? extends EnergyBill> bills)
	{
		return bills.stream()
				.filter(bill -> !bill.isDistanceMissing())
				.collect(Collectors.toMap(bill -> bill.getBill().getCarId(), EnergyBill::getDistance,
						BigDecimal::add));
	}

	private TotalStats calculateTotal(Bills bills)
	{
		return new TotalStats(
				sum(bills.fuel().stream().map(FuelBillEntity::getUnit)),
				sum(bills.charging().stream().map(ChargingBillEntity::getUnit)),
				trackedDistance(bills),
				sumTotal(bills.fuel()),
				sumTotal(bills.charging()),
				sumTotal(bills.maintenance()),
				sumTotal(bills.miscellaneous()),
				bills.total(),
				bills.fuel().size(),
				bills.charging().size(),
				bills.maintenance().size(),
				bills.miscellaneous().size());
	}

	private static BigDecimal sumTotal(List<? extends AbstractBillEntity> entities)
	{
		return sum(entities.stream().map(x -> x.getBill().getTotal()));
	}

	private static BigDecimal sum(Stream<BigDecimal> decimalStream)
	{
		return decimalStream.filter(Objects::nonNull).reduce(BigDecimal::add).orElse(ZERO);
	}

	private AverageStats calculateAverage(Bills bills)
	{
		return new AverageStats(
				average(bills.fuel(), FuelBillEntity::getPricePerUnit),
				average(bills.charging(), ChargingBillEntity::getPricePerUnit),
				average(bills.fuel(), FuelBillEntity::getDistance),
				average(bills.fuel(), FuelBillEntity::getCostPerKm),
				average(bills.charging(), ChargingBillEntity::getCostPerKm),
				average(bills.fuel(), FuelBillEntity::getAvgConsumption),
				average(bills.charging(), ChargingBillEntity::getAvgConsumption),
				average(bills.maintenance(), x -> x.getBill().getTotal()),
				calculateAverageCostPerKm(bills));
	}

	/**
	 * @return all costs per tracked km in cent
	 */
	private static BigDecimal calculateAverageCostPerKm(Bills bills)
	{
		BigDecimal total = bills.total();
		BigDecimal distance = trackedDistance(bills);
		if (!Utils.valueWasSet(total) || !Utils.valueWasSet(distance))
		{
			return BigDecimal.ZERO;
		}

		return total.divide(distance, 4, RoundingMode.HALF_UP).multiply(ONE_HUNDRED);
	}

	private static <T> BigDecimal average(List<T> billEntities, Function<T, BigDecimal> bigDecimalMapFunction)
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

	private HiLoStats calculateHighLow(Bills bills)
	{
		return new HiLoStats(
				hiLo(bills.fuel(), FuelBillEntity::getPricePerUnit, 1),
				hiLo(bills.charging(), ChargingBillEntity::getPricePerUnit, 1),
				hiLo(bills.fuel(), FuelBillEntity::getDistance, 2),
				hiLo(bills.fuel(), FuelBillEntity::getCostPerKm, 2),
				hiLo(bills.fuel(), FuelBillEntity::getAvgConsumption, 2),
				hiLo(bills.charging(), ChargingBillEntity::getAvgConsumption, 2),
				hiLo(bills.fuel(), x -> x.getBill().getTotal(), 2),
				hiLo(bills.charging(), x -> x.getBill().getTotal(), 2),
				hiLo(bills.maintenance(), x -> x.getBill().getTotal(), 2));
	}

	private static <T> HiLo hiLo(List<T> billEntities, Function<T, BigDecimal> bigDecimalFunction, int scale)
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
}
