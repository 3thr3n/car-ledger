package de.codeflowwizardry.carledger.data.repository;

import java.util.List;
import java.util.Map;

import de.codeflowwizardry.carledger.data.RecurringBillEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecurringBillRepository implements PanacheRepository<RecurringBillEntity>
{
	public List<RecurringBillEntity> getAll(long carId, String name)
	{
		return list("car.id = :carId and car.user.userId = :username order by nextDueDate desc",
				Map.of("carId", carId, "username", name));
	}

}
