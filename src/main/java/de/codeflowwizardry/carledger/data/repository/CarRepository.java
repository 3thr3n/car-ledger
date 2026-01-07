package de.codeflowwizardry.carledger.data.repository;

import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.CarEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
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
	public PanacheQuery<CarEntity> findAll()
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	public PanacheQuery<CarEntity> find(String query, Map<String, Object> params)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	public PanacheQuery<CarEntity> find(String query, Parameters params)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	public PanacheQuery<CarEntity> find(String query, Sort sort, Object... params)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	public PanacheQuery<CarEntity> find(String query, Sort sort, Map<String, Object> params)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	public PanacheQuery<CarEntity> find(String query, Sort sort, Parameters params)
	{
		throw new UnsupportedOperationException("Find is only allowed with user!");
	}

	@Override
	public PanacheQuery<CarEntity> findAll(Sort sort)
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
