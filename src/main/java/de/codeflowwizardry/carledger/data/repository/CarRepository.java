package de.codeflowwizardry.carledger.data.repository;

import java.util.List;
import java.util.Map;

import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Cars must always be looked up together with their user, use the methods with
 * a user name and not the generic
 * Panache ones. Access is checked by {@link CarAccess}, the methods with
 * <code>Owned</code> are about the owner only (car limit, backup).
 */
@ApplicationScoped
public class CarRepository implements PanacheRepository<CarEntity>
{
	/**
	 * Placeholder for "all cars of the user", where a car id is expected.
	 */
	public static final long ALL_CARS = -1;

	/**
	 * @return the car, if the user may do the action with it, otherwise
	 *         <code>null</code>
	 */
	public CarEntity findById(Long id, String name, Action action)
	{
		return find("id = :id and " + CarAccess.condition("this", action),
				Map.of("id", id, CarAccess.USER, name)).firstResult();
	}

	@Override
	public CarEntity findById(Long id)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	/**
	 * @return the cars the user may see
	 */
	public List<CarEntity> listForUser(String name)
	{
		return list(CarAccess.condition("this", Action.READ) + " order by id", Map.of(CarAccess.USER, name));
	}

	/**
	 * @return the cars of the user as owner
	 */
	public List<CarEntity> listOwned(String name)
	{
		return list("user.userId = ?1 order by id", name);
	}

	/**
	 * @return amount of cars of the user as owner, for the car limit
	 */
	public long countOwned(String name)
	{
		return count("user.userId", name);
	}

	@Override
	@Transactional
	public void persist(CarEntity carEntity)
	{
		PanacheRepository.super.persist(carEntity);
	}

	/**
	 * Deletes the car of the user, the database removes its bills and recurring
	 * bills as well.
	 *
	 * @return whether the car existed
	 */
	@Transactional
	public boolean deleteForUser(long id, String name)
	{
		return delete("id = :id and " + CarAccess.condition("this", Action.MANAGE),
				Map.of("id", id, CarAccess.USER, name)) == 1;
	}
}
