package de.codeflowwizardry.carledger.data.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.TechnicalInspectionEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TechnicalInspectionRepository implements PanacheRepository<TechnicalInspectionEntity>
{
	/**
	 * @return the inspections of the car, the latest first
	 */
	public List<TechnicalInspectionEntity> list(long carId, String username)
	{
		return find("from technical_inspection i join fetch i.car c where c.id = :carId and "
				+ CarAccess.condition("c", Action.READ) + " order by i.date desc, i.id desc",
				Map.of("carId", carId, CarAccess.USER, username))
				.list();
	}

	/**
	 * @return the inspection, if the user may do the action with its car
	 */
	public Optional<TechnicalInspectionEntity> find(long carId, long id, String username, Action action)
	{
		return find("from technical_inspection i join fetch i.car c where i.id = :id and c.id = :carId and "
				+ CarAccess.condition("c", action),
				Map.of("id", id, "carId", carId, CarAccess.USER, username))
				.firstResultOptional();
	}

	/**
	 * @return the latest inspection of the car, the last added on the same day
	 */
	public Optional<TechnicalInspectionEntity> latest(long carId)
	{
		return find("car.id = ?1 order by date desc, id desc", carId).firstResultOptional();
	}
}
