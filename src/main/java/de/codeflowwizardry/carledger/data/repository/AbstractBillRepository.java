package de.codeflowwizardry.carledger.data.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.AbstractBillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
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

	/**
	 * Fields of the bill (alias <code>b</code>) that every list can be sorted by,
	 * by their name in the API.
	 */
	private static final Map<String, String> BILL_SORT_FIELDS = Map.of(
			"date", "b.date",
			"total", "b.total",
			"netAmount", "b.netAmount",
			"ustAmount", "b.ustAmount",
			"vatRate", "b.vatRate");

	/**
	 * @return the fields the bills can be sorted by: their name in the API mapped
	 *         to the path in the query
	 */
	public Map<String, String> getSortFields()
	{
		return BILL_SORT_FIELDS;
	}

	/**
	 * @return the start of the query, the bill has the alias <code>b</code>, the
	 *         type specific part <code>e</code>
	 */
	protected String fromClause()
	{
		// fetch the bill directly, otherwise it is loaded by an extra select for every
		// row
		return "from " + entityName + " e join fetch e.bill b";
	}

	/**
	 * @param sortField
	 *            name of the field in the API, one of {@link #getSortFields()}
	 * @throws IllegalArgumentException
	 *             if the bills cannot be sorted by the field
	 */
	public PanacheQuery<T> getBills(long carId, String username, Page page, Optional<Integer> year,
			String sortField, boolean ascending)
	{
		String sortPath = getSortFields().get(sortField);
		if (sortPath == null)
		{
			throw new IllegalArgumentException("Cannot sort by " + sortField);
		}

		String query = fromClause() + " where b.car.id = :carId and " + CarAccess.condition("b.car", Action.READ);
		Map<String, Object> params = new HashMap<>();
		params.put("carId", carId);
		params.put(CarAccess.USER, username);

		if (year.isPresent())
		{
			query += " and year(b.date) = :year";
			params.put("year", year.get());
		}

		// newest first within equal values and the id, so paging is stable
		String direction = ascending ? "asc" : "desc";
		query += " order by " + sortPath + " " + direction + " nulls last, b.date desc, b.id desc";

		return find(query, params).page(page);
	}

	/**
	 * Adds the given fields to the ones every bill can be sorted by.
	 */
	protected static Map<String, String> withBillSortFields(Map<String, String> fields)
	{
		Map<String, String> all = new HashMap<>(BILL_SORT_FIELDS);
		all.putAll(fields);
		return Map.copyOf(all);
	}

	/**
	 * @return the bill, if it belongs to the car and the user may do the action
	 *         with the car
	 */
	public Optional<T> findBill(long carId, long billId, String username, Action action)
	{
		return find("from " + entityName + " e join fetch e.bill b"
				+ " where b.id = :billId and b.car.id = :carId and " + CarAccess.condition("b.car", action),
				Map.of("billId", billId, "carId", carId, CarAccess.USER, username))
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
				+ " where b.car.id = :carId and " + CarAccess.condition("b.car", Action.READ);
		if (type != null)
		{
			query += " and b.type = :type";
		}

		var typedQuery = getEntityManager()
				.createQuery(query + " order by year(b.date) desc", Integer.class)
				.setParameter("carId", carId)
				.setParameter(CarAccess.USER, username);
		if (type != null)
		{
			typedQuery.setParameter("type", type);
		}
		return typedQuery.getResultList();
	}

	/**
	 * @return all bills of the car, optionally of one year, the oldest first
	 */
	public List<T> listBills(long carId, String username, Optional<Integer> year)
	{
		String query = "from " + entityName + " e join fetch e.bill b"
				+ " where b.car.id = :carId and " + CarAccess.condition("b.car", Action.READ);
		Map<String, Object> params = new HashMap<>();
		params.put("carId", carId);
		params.put(CarAccess.USER, username);

		if (year.isPresent())
		{
			query += " and year(b.date) = :year";
			params.put("year", year.get());
		}

		return find(query + " order by b.date asc, b.id asc", params).list();
	}
}
