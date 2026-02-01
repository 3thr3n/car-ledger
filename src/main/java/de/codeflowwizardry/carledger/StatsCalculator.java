package de.codeflowwizardry.carledger;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import de.codeflowwizardry.carledger.data.AbstractBillEntity;
import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.data.MaintenanceBillEntity;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
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

	private final BillRepository repository;

	@Inject
	public StatsCalculator(BillRepository billRepository)
	{
		this.repository = billRepository;
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
	public TotalStats calculateTotal(Long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
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

		BigDecimal unit = calculateTotalUnit(fuelBillList);
		BigDecimal trackedDistance = calculateTotalDistance(fuelBillList);
		BigDecimal fuelTotal = calculateTotalFuel(fuelBillList);
		BigDecimal maintenanceTotal = calculateTotalMaintenance(maintenanceBillList);
		BigDecimal total = fuelTotal.add(maintenanceTotal);
		Integer fuelBills = fuelBillList.size();
		Integer maintenanceBills = maintenanceBillList.size();

		return new TotalStats(unit, trackedDistance, fuelTotal, maintenanceTotal, total, fuelBills, maintenanceBills);
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

	private static BigDecimal calculateTotalFuel(List<FuelBillEntity> entities)
	{
		Stream<BigDecimal> totalCost = entities.stream().map(x -> x.getBill().getTotal());
		return handleReduceTotalResult(totalCost);
	}

	private static BigDecimal calculateTotalMaintenance(List<MaintenanceBillEntity> entities)
	{
		Stream<BigDecimal> totalCost = entities.stream().map(x -> x.getBill().getTotal());
		return handleReduceTotalResult(totalCost);
	}

	private static BigDecimal handleReduceTotalResult(Stream<BigDecimal> decimalStream)
	{
		return decimalStream.reduce(BigDecimal::add).orElse(ZERO);
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
	public AverageStats calculateAverage(Long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
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

		// Price per unit
		BigDecimal pricePerUnit = calculateAveragePricePerUnit(fuelBillList);
		// Distance per fill-up
		BigDecimal distance = calculateAverageDistance(fuelBillList);
		// Cost per km TODO
		BigDecimal fuelCostPerKm = calculateAverageFuelCostPerKm(fuelBillList);
		// Fuel consumption (L/100km)
		BigDecimal consumption = calculateAverageConsumption(fuelBillList);
		// Maintenance cost per bill
		BigDecimal averageMaintenancePrice = calculateAverageMaintenance(maintenanceBillList);
		// Cost per km (total, including maintenance)
		BigDecimal costPerKm = calculateAverageCostPerKm(fuelBillList, maintenanceBillList);

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
			List<MaintenanceBillEntity> maintenanceBillList)
	{
		BigDecimal maintenance = calculateTotalMaintenance(maintenanceBillList);
		BigDecimal fuel = calculateTotalFuel(fuelBillEntities);

		BigDecimal distance = calculateTotalDistance(fuelBillEntities);

		BigDecimal total = maintenance.add(fuel);

		if (!Utils.valueWasSet(total))
		{
			return BigDecimal.ZERO;
		}

		return total.divide(distance, 2, RoundingMode.HALF_UP);
	}

	private static <T extends AbstractBillEntity> BigDecimal handleReduceAverageResult(List<T> billEntities,
			Function<T, BigDecimal> bigDecimalMapFunction)
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
		List<BillEntity> billEntities = repository.getBills(carId, username, from, to);

		List<FuelBillEntity> fuelBillList = billEntities.stream()
				.map(BillEntity::getFuelBill)
				.filter(Objects::nonNull)
				.toList();

		List<MaintenanceBillEntity> maintenanceBillList = billEntities.stream()
				.map(BillEntity::getMaintenanceBill)
				.filter(Objects::nonNull)
				.toList();

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
				.min(Comparator.naturalOrder())
				.orElse(ZERO);

		BigDecimal max = billEntities.stream()
				.map(bigDecimalFunction)
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
