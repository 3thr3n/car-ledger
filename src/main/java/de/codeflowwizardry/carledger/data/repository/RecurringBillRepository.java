package de.codeflowwizardry.carledger.data.repository;

import java.util.HashMap;
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
	public PanacheQuery<RecurringBillEntity> getAll(long carId, String name)
	{
		Map<String, Object> params = new HashMap<>();
		String query = "car.user.userId = :username";
		params.put("username", name);

		if (carId >= 0)
		{
			query += " and car.id = :carId";
			params.put("carId", carId);
		}

		return find(query, params);
	}

	public PanacheQuery<RecurringBillEntity> getAll(long carId, String name, Page queryPage, boolean running)
	{
		Map<String, Object> params = new HashMap<>();
		String query = "car.id = :carId and car.user.userId = :username";
		params.put("carId", carId);
		params.put("username", name);

		if (running)
		{
			query += " and (nextDueDate < endDate or endDate is null)";
		}

		query += " order by nextDueDate desc";

		return find(query, params).page(queryPage);
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
