package de.codeflowwizardry.carledger.data.repository;

import java.time.LocalDate;
import java.util.*;

import de.codeflowwizardry.carledger.data.BillEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BillRepository implements PanacheRepository<BillEntity>
{
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
			query += " and day >= :from and day <= :to";
			params.put("from", from.get());
			params.put("to", to.get());

		} else if (from.isPresent())
		{
			query += " and day >= :from";
			params.put("from", from.get());

		} else if (to.isPresent())
		{
			query += " and day <= :to";
			params.put("to", to.get());
		}

		query += " order by day desc";

		return find(query, params).list();
	}

	public List<Integer> getBillYears(long carId, String username)
	{
		return find("select b.day from Bill b where b.car.id = ?1 and b.car.user.userId = ?2", carId, username)
				.project(LocalDate.class)
				.list()
				.stream()
				.map(LocalDate::getYear)
				.distinct()
				.sorted(Comparator.reverseOrder())
				.toList();
	}

	public PanacheQuery<BillEntity> getBills(long carId, String username, Page page, Optional<Integer> year)
	{
		String query = "car.id = :carId and car.user.userId = :username order by day desc";
		Map<String, Object> params = new HashMap<>();
		params.put("carId", carId);
		params.put("username", username);

		if (year.isPresent())
		{
			query = "year(day) = :year and " + query;
			params.put("year", year.get());
		}

		return find(query, params)
				.page(page);
	}

	public Optional<BillEntity> getBillById(long billId, long carId, String username)
	{
		return find("id = ?1 and car.id = ?2 and car.user.userId = ?3 order by day desc", billId, carId, username)
				.firstResultOptional();
	}

	@Override
	public boolean isPersistent(BillEntity entity)
	{
		return find("day = ?1 and unit = ?2 and car.id = ?3 and distance = ?4", entity.getDay(), entity.getUnit(),
				entity.getCarId(), entity.getDistance())
				.firstResultOptional().isPresent();
	}

	@Override
	@Transactional
	public void delete(BillEntity billEntity)
	{
		PanacheRepository.super.delete(billEntity);
	}

	@Override
	@Transactional
	public void persist(BillEntity billEntity)
	{
		PanacheRepository.super.persist(billEntity);
	}
}
