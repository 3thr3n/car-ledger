package de.codeflowwizardry.carledger.data.repository;

import static de.codeflowwizardry.carledger.data.repository.CarRepository.ALL_CARS;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.exception.DbDeletionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BillRepository extends AbstractBillRepository<BillEntity>
{
	public BillRepository()
	{
		super("bill");
	}

	private static final Map<String, String> SORT_FIELDS = withBillSortFields(Map.of("type", "b.type"));

	@Override
	public Map<String, String> getSortFields()
	{
		return SORT_FIELDS;
	}

	@Override
	protected String fromClause()
	{
		return "from bill b";
	}

	/**
	 * Loads the bills including their type specific part in one query.
	 *
	 * @param carId
	 *            or {@link CarRepository#ALL_CARS}
	 */
	public List<BillEntity> getBills(long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
	{
		Map<String, Object> params = new HashMap<>();

		String query = "from bill b"
				+ " left join fetch b.fuelBill"
				+ " left join fetch b.chargingBill"
				+ " left join fetch b.maintenanceBill"
				+ " left join fetch b.miscellaneousBill"
				+ where(carId, username, from, to, params);

		return find(query + " order by b.date desc", params).list();
	}

	/**
	 * @return amount of bills per car id, cars without bills are missing
	 */
	public Map<Long, Long> countBillsPerCar(String username)
	{
		return getEntityManager()
				.createQuery(
						"select b.car.id, count(b) from bill b where " + CarAccess.condition("b.car", Action.READ)
								+ " group by b.car.id",
						Object[].class)
				.setParameter(CarAccess.USER, username)
				.getResultList()
				.stream()
				.collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
	}

	/**
	 * @return amount and total of the bills of a car, grouped by type. Types
	 *         without bills are missing.
	 */
	public List<BillTypeSummary> summarizeByType(long carId, String username)
	{
		return getEntityManager()
				.createQuery("select b.type, count(b), sum(b.total) from bill b"
						+ " where b.car.id = :carId and " + CarAccess.condition("b.car", Action.READ)
						+ " group by b.type", Object[].class)
				.setParameter("carId", carId)
				.setParameter(CarAccess.USER, username)
				.getResultList()
				.stream()
				.map(row -> new BillTypeSummary((BillType) row[0], (Long) row[1], (BigDecimal) row[2]))
				.toList();
	}

	/**
	 * @param energy
	 *            the refuels in l/100km or the charges in kWh/100km
	 * @return average consumption of all refuels or charges of a car, which have a
	 *         consumption
	 */
	public BigDecimal averageConsumption(long carId, String username, Energy energy)
	{
		Object[] sumAndCount = getEntityManager()
				.createQuery("select sum(f.avgConsumption), count(f) from " + energy.entity + " f"
						+ " where f.bill.car.id = :carId and " + CarAccess.condition("f.bill.car", Action.READ)
						+ " and f.avgConsumption > 0", Object[].class)
				.setParameter("carId", carId)
				.setParameter(CarAccess.USER, username)
				.getSingleResult();

		long count = (Long) sumAndCount[1];
		if (count == 0)
		{
			return BigDecimal.ZERO;
		}
		return ((BigDecimal) sumAndCount[0]).divide(BigDecimal.valueOf(count), RoundingMode.HALF_UP);
	}

	/**
	 * @param carId
	 *            or {@link CarRepository#ALL_CARS}
	 * @return total of the bills per month and type, months without bills are
	 *         missing
	 */
	public List<MonthTotal> sumPerMonth(long carId, String username, Optional<LocalDate> from,
			Optional<LocalDate> to)
	{
		Map<String, Object> params = new HashMap<>();
		String query = "select year(b.date), month(b.date), b.type, sum(b.total) from bill b"
				+ where(carId, username, from, to, params)
				+ " group by year(b.date), month(b.date), b.type";

		return rows(query, params).stream()
				.map(row -> new MonthTotal(month(row), (BillType) row[2], (BigDecimal) row[3]))
				.toList();
	}

	/**
	 * Liters or kWh and distance per month and car. Consumption and price only use
	 * refuels or charges with a distance, the others would distort them.
	 *
	 * @param carId
	 *            or {@link CarRepository#ALL_CARS}
	 * @return months without refuels or charges are missing
	 */
	public List<MonthEnergy> sumEnergyPerMonth(long carId, String username, Optional<LocalDate> from,
			Optional<LocalDate> to, Energy energy)
	{
		Map<String, Object> params = new HashMap<>();
		String query = "select year(b.date), month(b.date), b.car.id, sum(f.unit), sum(coalesce(f.distance, 0)),"
				+ " sum(case when f.distance > 0 then f.unit else 0 end),"
				+ " sum(f.pricePerUnit * f.unit)"
				+ " from bill b join b." + energy.association + " f"
				+ where(carId, username, from, to, params)
				+ " group by year(b.date), month(b.date), b.car.id";

		return rows(query, params).stream()
				.map(row -> new MonthEnergy(month(row), (Long) row[2], (BigDecimal) row[3], (BigDecimal) row[4],
						(BigDecimal) row[5], (BigDecimal) row[6]))
				.toList();
	}

	private static String where(long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to,
			Map<String, Object> params)
	{
		return PeriodFilter.where("b.car", "b.date", carId, username, from, to, params);
	}

	private List<Object[]> rows(String query, Map<String, Object> params)
	{
		var typedQuery = getEntityManager().createQuery(query, Object[].class);
		params.forEach(typedQuery::setParameter);
		return typedQuery.getResultList();
	}

	private static YearMonth month(Object[] row)
	{
		return YearMonth.of((Integer) row[0], (Integer) row[1]);
	}

	@Transactional
	public void delete(long carId, long billId, String username)
	{
		Optional<BillEntity> any = find(
				"id = :billId and car.id = :carId and " + CarAccess.condition("car", Action.WRITE),
				Map.of("carId", carId, CarAccess.USER, username, "billId", billId))
				.firstResultOptional();

		if (any.isEmpty())
		{
			throw new DbDeletionException("Bill could not be deleted (not found)");
		}

		delete(any.get());
	}

	public record BillTypeSummary(BillType type, long count, BigDecimal total)
	{
	}

	public record MonthTotal(YearMonth month, BillType type, BigDecimal total)
	{
	}

	/**
	 * @param unit
	 *            liters or kWh
	 * @param unitWithDistance
	 *            liters or kWh of the refuels or charges with a distance
	 * @param pricedUnit
	 *            sum of price per unit (cent) times unit
	 */
	public record MonthEnergy(YearMonth month, long carId, BigDecimal unit, BigDecimal distance,
			BigDecimal unitWithDistance, BigDecimal pricedUnit)
	{
	}

	/**
	 * Refuels in liters or charges in kWh
	 */
	public enum Energy {
		FUEL("FuelBillEntity", "fuelBill"), CHARGING("ChargingBillEntity", "chargingBill");

		private final String entity;
		private final String association;

		Energy(String entity, String association)
		{
			this.entity = entity;
			this.association = association;
		}
	}
}
