package de.codeflowwizardry.carledger.data.repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.codeflowwizardry.carledger.data.RecurringBillEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RecurringBillRepository implements PanacheRepository<RecurringBillEntity>
{
	/**
	 * a bill is running until its next payment would be on or after the end date
	 */
	private static final String RUNNING = "(endDate is null or nextDueDate < endDate)";

	/**
	 * @param byNextDue
	 *            the next due payment first, otherwise running bills before
	 *            finished ones, the newest (by id, ids are assigned in order of
	 *            creation) first
	 */
	public PanacheQuery<RecurringBillEntity> getAll(long carId, String name, Page queryPage, boolean running,
			boolean byNextDue)
	{
		Map<String, Object> params = new HashMap<>();
		String query = "car.id = :carId and car.user.userId = :username";
		params.put("carId", carId);
		params.put("username", name);

		if (running)
		{
			query += " and " + RUNNING;
		}

		if (byNextDue)
		{
			query += " order by nextDueDate asc, id asc";
		} else
		{
			query += " order by case when " + RUNNING + " then 0 else 1 end, id desc";
		}

		return find(query, params).page(queryPage);
	}

	/**
	 * @return ids of all bills with a payment due on or before the given day
	 */
	public List<Long> listDueIds(LocalDate day)
	{
		return getEntityManager()
				.createQuery("select r.id from recurring_bill r where r.nextDueDate <= :day"
						+ " and (r.endDate is null or r.nextDueDate < r.endDate) order by r.id", Long.class)
				.setParameter("day", day)
				.getResultList();
	}

	public RecurringBillEntity getById(long carId, String name, long billId)
	{
		return find("id = :id and car.id = :carId and car.user.userId = :username",
				Map.of("id", billId, "carId", carId, "username", name)).firstResult();
	}

	@Transactional
	public boolean deleteById(long carId, long billId, String name)
	{
		long delete = delete("id = :id and car.id = :carId and car.user.userId = :username",
				Map.of("id", billId, "carId", carId, "username", name));

		return delete == 1;
	}
}
