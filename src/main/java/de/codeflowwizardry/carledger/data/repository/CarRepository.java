package de.codeflowwizardry.carledger.data.repository;

import java.util.Optional;

import de.codeflowwizardry.carledger.data.CarEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CarRepository implements PanacheRepository<CarEntity>
{
	public CarEntity findById(Long id, String name)
	{
		return find("id = ?1 and user.userId = ?2", id, name).firstResult();
	}

	@Override
	public CarEntity findById(Long id)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	public CarEntity findById(Long id, LockModeType lockModeType)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	public Optional<CarEntity> findByIdOptional(Long id)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	public Optional<CarEntity> findByIdOptional(Long id, LockModeType lockModeType)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	@Transactional
	public void persist(CarEntity carEntity)
	{
		PanacheRepository.super.persist(carEntity);
	}
}
