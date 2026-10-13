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
						"select b.car.id, count(b) from bill b where b.car.user.userId = :userId group by b.car.id",
						Object[].class)
				.setParameter("userId", username)
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
						+ " where b.car.id = :carId and b.car.user.userId = :userId group by b.type", Object[].class)
				.setParameter("carId", carId)
				.setParameter("userId", username)
				.getResultList()
				.stream()
				.map(row -> new BillTypeSummary((BillType) row[0], (Long) row[1], (BigDecimal) row[2]))
				.toList();
	}

	/**
	 * @return average consumption of all fuel bills of a car, which have a
	 *         consumption
	 */
	public BigDecimal averageConsumption(long carId, String username)
	{
		Object[] sumAndCount = getEntityManager()
				.createQuery("select sum(f.avgConsumption), count(f) from FuelBillEntity f"
						+ " where f.bill.car.id = :carId and f.bill.car.user.userId = :userId"
						+ " and f.avgConsumption > 0", Object[].class)
				.setParameter("carId", carId)
				.setParameter("userId", username)
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
	 * Liters and distance per month. Consumption and price only use refuels with a
	 * distance, the others would distort them.
	 *
	 * @param carId
	 *            or {@link CarRepository#ALL_CARS}
	 * @return months without refuels are missing
	 */
	public List<MonthFuel> sumFuelPerMonth(long carId, String username, Optional<LocalDate> from,
			Optional<LocalDate> to)
	{
		Map<String, Object> params = new HashMap<>();
		String query = "select year(b.date), month(b.date), sum(f.unit), sum(coalesce(f.distance, 0)),"
				+ " sum(case when f.distance > 0 then f.unit else 0 end),"
				+ " sum(f.pricePerUnit * f.unit)"
				+ " from bill b join b.fuelBill f"
				+ where(carId, username, from, to, params)
				+ " group by year(b.date), month(b.date)";

		return rows(query, params).stream()
				.map(row -> new MonthFuel(month(row), (BigDecimal) row[2], (BigDecimal) row[3],
						(BigDecimal) row[4], (BigDecimal) row[5]))
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
		Optional<BillEntity> any = find("id = :billId and car.id = :carId and car.user.userId = :userId",
				Map.of("carId", carId, "userId", username, "billId", billId))
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
	 * @param unitWithDistance
	 *            liters of the refuels with a distance
	 * @param pricedUnit
	 *            sum of price per liter (cent) times liters
	 */
	public record MonthFuel(YearMonth month, BigDecimal unit, BigDecimal distance, BigDecimal unitWithDistance,
			BigDecimal pricedUnit)
	{
	}
}
