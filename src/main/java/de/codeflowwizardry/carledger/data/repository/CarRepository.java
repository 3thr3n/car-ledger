package de.codeflowwizardry.carledger.data.repository;

import java.util.List;

import de.codeflowwizardry.carledger.data.CarEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Cars must always be looked up together with their user, use the methods with
 * a user name and not the generic
 * Panache ones.
 */
@ApplicationScoped
public class CarRepository implements PanacheRepository<CarEntity>
{
	/**
	 * Placeholder for "all cars of the user", where a car id is expected.
	 */
	public static final long ALL_CARS = -1;

	public CarEntity findById(Long id, String name)
	{
		return find("id = ?1 and user.userId = ?2", id, name).firstResult();
	}

	@Override
	public CarEntity findById(Long id)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	public List<CarEntity> listForUser(String name)
	{
		return list("user.userId = ?1 order by id", name);
	}

	public long countForUser(String name)
	{
		return count("user.userId", name);
	}

	@Override
	@Transactional
	public void persist(CarEntity carEntity)
	{
		PanacheRepository.super.persist(carEntity);
	}
}
