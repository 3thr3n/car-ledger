package de.codeflowwizardry.carledger.data.repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.codeflowwizardry.carledger.data.RecurringBillEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
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
	 *            finished ones, the latest first payment (start date) first, so
	 *            the longest running is at the bottom
	 */
	public PanacheQuery<RecurringBillEntity> getAll(long carId, String name, Page queryPage, boolean running,
			boolean byNextDue)
	{
		Map<String, Object> params = new HashMap<>();
		String query = "car.id = :carId and " + CarAccess.condition("car", Action.READ);
		params.put("carId", carId);
		params.put(CarAccess.USER, name);

		if (running)
		{
			query += " and " + RUNNING;
		}

		if (byNextDue)
		{
			query += " order by nextDueDate asc, id asc";
		} else
		{
			query += " order by case when " + RUNNING + " then 0 else 1 end, startDate desc, id desc";
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

	/**
	 * @return the bill, if the user may do the action with its car, otherwise
	 *         <code>null</code>
	 */
	public RecurringBillEntity getById(long carId, String name, long billId, Action action)
	{
		return find("id = :id and car.id = :carId and " + CarAccess.condition("car", action),
				Map.of("id", billId, "carId", carId, CarAccess.USER, name)).firstResult();
	}

	@Transactional
	public boolean deleteById(long carId, long billId, String name)
	{
		long delete = delete("id = :id and car.id = :carId and " + CarAccess.condition("car", Action.WRITE),
				Map.of("id", billId, "carId", carId, CarAccess.USER, name));

		return delete == 1;
	}
}
