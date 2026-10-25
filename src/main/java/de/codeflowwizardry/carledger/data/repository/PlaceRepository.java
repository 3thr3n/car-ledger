package de.codeflowwizardry.carledger.data.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.PlaceEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PlaceRepository implements PanacheRepository<PlaceEntity>
{
	/**
	 * @return the named places of the car, oldest first
	 */
	public List<PlaceEntity> list(CarEntity car)
	{
		return list("car = ?1 order by id", car);
	}

	/**
	 * @return the place, if the user may do the action with its car
	 */
	public Optional<PlaceEntity> find(long carId, long id, String userId, Action action)
	{
		return find("id = :id and car.id = :carId and " + CarAccess.condition("car", action),
				Map.of("id", id, "carId", carId, CarAccess.USER, userId))
				.firstResultOptional();
	}
}
