package de.codeflowwizardry.carledger.data.repository;

import java.util.List;
import java.util.Map;

import de.codeflowwizardry.carledger.data.RecurringBillEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RecurringBillRepository implements PanacheRepository<RecurringBillEntity>
{
	public List<RecurringBillEntity> getAll(long carId, String name)
	{
		return list("car.id = :carId and car.user.userId = :username order by nextDueDate desc",
				Map.of("carId", carId, "username", name));
	}

	public RecurringBillEntity getById(long carId, String name, long billId)
	{
		return find("id = :id and car.id = :carId and car.user.userId = :username",
				Map.of("id", billId, "carId", carId, "username", name)).firstResult();
	}

	@Transactional
	public boolean deleteById(long carId, String name, long billId)
	{
		long delete = delete("id = :id and car.id = :carId and car.user.userId = :username",
				Map.of("id", billId, "carId", carId, "username", name));

		return delete == 1;
	}
}
