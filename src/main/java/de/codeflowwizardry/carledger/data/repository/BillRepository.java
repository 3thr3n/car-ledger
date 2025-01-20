package de.codeflowwizardry.carledger.data.repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.Bill;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BillRepository implements PanacheRepository<Bill>
{
	public List<Bill> getBills(long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("carId", carId);
		params.put("userId", username);

		String query = "car.id = :carId and car.user.userId = :userId";

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

	public PanacheQuery<Bill> getBills(long carId, String username, Page page)
	{
		return find("car.id = ?1 and car.user.userId = ?2 order by day desc", carId, username)
				.page(page);
	}

	public Optional<Bill> getBillById(long billId, long carId, String username)
	{
		return find("id = ?1 and car.id = ?2 and car.user.userId = ?3 order by day desc", billId, carId, username)
				.firstResultOptional();
	}

	@Override
	public boolean isPersistent(Bill entity)
	{
		return find("day = ?1 and unit = ?2 and car.id = ?3 and distance = ?4", entity.getDay(), entity.getUnit(),
				entity.getCarId(), entity.getDistance())
				.firstResultOptional().isPresent();
	}

	@Override
	@Transactional
	public void delete(Bill bill)
	{
		PanacheRepository.super.delete(bill);
	}

	@Override
	@Transactional
	public void persist(Bill bill)
	{
		PanacheRepository.super.persist(bill);
	}
}
