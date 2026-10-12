package de.codeflowwizardry.carledger.data.repository;

import static de.codeflowwizardry.carledger.data.repository.CarRepository.ALL_CARS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.ReminderEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReminderRepository implements PanacheRepository<ReminderEntity>
{
	/**
	 * @param carId
	 *            or {@link CarRepository#ALL_CARS}
	 * @return the reminders including their car
	 */
	public List<ReminderEntity> list(long carId, String username)
	{
		Map<String, Object> params = new HashMap<>();
		String query = "from reminder r join fetch r.car c where c.user.userId = :userId";
		params.put("userId", username);

		if (carId != ALL_CARS)
		{
			query += " and c.id = :carId";
			params.put("carId", carId);
		}
		return find(query, params).list();
	}

	public Optional<ReminderEntity> find(long carId, long id, String username)
	{
		return find("from reminder r join fetch r.car c where r.id = :id and c.id = :carId and c.user.userId = :userId",
				Parameters.with("id", id).and("carId", carId).and("userId", username))
				.firstResultOptional();
	}
}
