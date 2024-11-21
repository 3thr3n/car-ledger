package de.codeflowwizardry.carledger.data.repository;

import java.util.Optional;

import de.codeflowwizardry.carledger.data.Car;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CarRepository implements PanacheRepository<Car>
{
	public Car findById(Long id, String name)
	{
		return find("id = ?1 and user.userId = ?2", id, name).firstResult();
	}

	@Override
	public Car findById(Long id)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	public Car findById(Long id, LockModeType lockModeType)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	public Optional<Car> findByIdOptional(Long id)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	public Optional<Car> findByIdOptional(Long id, LockModeType lockModeType)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	@Transactional
	public void persist(Car car)
	{
		PanacheRepository.super.persist(car);
	}
}
