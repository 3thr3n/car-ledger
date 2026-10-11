package de.codeflowwizardry.carledger.data.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.AbstractBillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;

/**
 * Base for the repositories of the type specific bills, which all reference
 * their bill as
 * <code>bill</code>.
 */
public abstract class AbstractBillRepository<T extends AbstractBillEntity> implements PanacheRepository<T>
{
	private final String entityName;

	protected AbstractBillRepository(String entityName)
	{
		this.entityName = entityName;
	}

	public PanacheQuery<T> getBills(long carId, String username, Page page, Optional<Integer> year)
	{
		// fetch the bill directly, otherwise it is loaded by an extra select for every
		// row
		String query = "from " + entityName + " e join fetch e.bill b"
				+ " where b.car.id = :carId and b.car.user.userId = :username";
		Map<String, Object> params = new HashMap<>();
		params.put("carId", carId);
		params.put("username", username);

		if (year.isPresent())
		{
			query += " and year(b.date) = :year";
			params.put("year", year.get());
		}

		return find(query + " order by b.date desc", params).page(page);
	}

	/**
	 * @return the bill, if it belongs to the car of the user
	 */
	public Optional<T> findBill(long carId, long billId, String username)
	{
		return find("from " + entityName + " e join fetch e.bill b"
				+ " where b.id = :billId and b.car.id = :carId and b.car.user.userId = :username",
				Map.of("billId", billId, "carId", carId, "username", username))
				.firstResultOptional();
	}

	/**
	 * @param type
	 *            to filter for, <code>null</code> for bills of all types
	 * @return all years with bills, newest first
	 */
	public List<Integer> getBillYears(long carId, String username, BillType type)
	{
		String query = "select distinct year(b.date) from bill b"
				+ " where b.car.id = :carId and b.car.user.userId = :username";
		if (type != null)
		{
			query += " and b.type = :type";
		}

		var typedQuery = getEntityManager()
				.createQuery(query + " order by year(b.date) desc", Integer.class)
				.setParameter("carId", carId)
				.setParameter("username", username);
		if (type != null)
		{
			typedQuery.setParameter("type", type);
		}
		return typedQuery.getResultList();
	}
}
