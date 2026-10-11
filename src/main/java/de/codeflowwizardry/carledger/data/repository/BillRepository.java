package de.codeflowwizardry.carledger.data.repository;

import static de.codeflowwizardry.carledger.data.repository.CarRepository.ALL_CARS;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.exception.DbDeletionException;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BillRepository extends AbstractBillRepository<BillEntity>
{
	public BillRepository()
	{
		super("bill");
	}

	@Override
	public PanacheQuery<BillEntity> getBills(long carId, String username, Page page, Optional<Integer> year)
	{
		String query = "car.id = :carId and car.user.userId = :username";
		Map<String, Object> params = new HashMap<>();
		params.put("carId", carId);
		params.put("username", username);

		if (year.isPresent())
		{
			query += " and year(date) = :year";
			params.put("year", year.get());
		}

		return find(query + " order by date desc", params).page(page);
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
				+ " where b.car.user.userId = :userId";
		params.put("userId", username);

		if (carId != ALL_CARS)
		{
			query += " and b.car.id = :carId";
			params.put("carId", carId);
		}

		if (from.isPresent())
		{
			query += " and b.date >= :from";
			params.put("from", from.get());
		}

		if (to.isPresent())
		{
			query += " and b.date <= :to";
			params.put("to", to.get());
		}

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

	@Transactional
	public void delete(long carId, long billId, String username)
	{
		Optional<BillEntity> any = find("id = :billId and car.id = :carId and car.user.userId = :userId",
				Parameters.with("carId", carId).and("userId", username).and("billId", billId))
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
}
