package de.codeflowwizardry.carledger.data.repository;

import java.time.LocalDate;
import java.util.*;

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
	@Override
	public List<Integer> getBillYears(long carId, String username, BillType type)
	{
		return find("select date from bill where car.id = :carId and car.user.userId = :userId",
				Parameters.with("carId", carId).and("userId", username))
				.project(LocalDate.class)
				.list()
				.stream()
				.map(LocalDate::getYear)
				.distinct()
				.sorted(Comparator.reverseOrder())
				.toList();
	}

	@Override
	public PanacheQuery<BillEntity> getBills(long carId, String username, Page page, Optional<Integer> year)
	{
		String query = "car.id = :carId and car.user.userId = :username order by date desc";
		Map<String, Object> params = new HashMap<>();
		params.put("carId", carId);
		params.put("username", username);

		if (year.isPresent())
		{
			query = "year(date) = :year and " + query;
			params.put("year", year.get());
		}

		return find(query, params)
				.page(page);
	}

	public List<BillEntity> getBills(long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
	{
		Map<String, Object> params = new HashMap<>();

		String query = "car.user.userId = :userId";
		params.put("userId", username);

		if (carId >= 0)
		{
			query += " and car.id = :carId";
			params.put("carId", carId);
		}

		if (from.isPresent() && to.isPresent())
		{
			query += " and date >= :from and date <= :to";
			params.put("from", from.get());
			params.put("to", to.get());

		} else if (from.isPresent())
		{
			query += " and date >= :from";
			params.put("from", from.get());
		}

		query += " order by date desc";

		return find(query, params).list();
	}

	@Transactional
	public void delete(long carId, long billId, String username)
	{
		Optional<BillEntity> any = find("id = :billId and car.id = :carId and car.user.userId = :userId",
				Parameters.with("carId", carId).and("userId", username).and("billId", billId))
				.stream().findAny();

		if (any.isEmpty())
		{
			throw new DbDeletionException("Bill could not be deleted (not found)");
		}

		delete(any.get());
	}
}
