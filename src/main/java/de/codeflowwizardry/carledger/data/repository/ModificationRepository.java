package de.codeflowwizardry.carledger.data.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.ModificationEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ModificationRepository implements PanacheRepository<ModificationEntity>
{
	/**
	 * @return the modifications of the car including their bill, the installed
	 *         ones first, then the latest installed
	 */
	public List<ModificationEntity> list(long carId, String username)
	{
		return find("from modification m join fetch m.car c left join fetch m.bill where c.id = :carId and "
				+ CarAccess.condition("c", Action.READ)
				+ " order by case when m.removedDate is null then 0 else 1 end, m.installedDate desc nulls last,"
				+ " m.id desc",
				Map.of("carId", carId, CarAccess.USER, username))
				.list();
	}

	/**
	 * @return the modification, if the user may do the action with its car
	 */
	public Optional<ModificationEntity> find(long carId, long id, String username, Action action)
	{
		return find("from modification m join fetch m.car c left join fetch m.bill where m.id = :id"
				+ " and c.id = :carId and " + CarAccess.condition("c", action),
				Map.of("id", id, "carId", carId, CarAccess.USER, username))
				.firstResultOptional();
	}
}
