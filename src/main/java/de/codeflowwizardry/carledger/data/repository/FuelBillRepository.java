package de.codeflowwizardry.carledger.data.repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.FuelBillEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class FuelBillRepository extends AbstractBillRepository<FuelBillEntity>
{
	private final BillRepository billRepository;

	@Inject
	public FuelBillRepository(BillRepository billRepository)
	{
		this.billRepository = billRepository;
	}

	public List<FuelBillEntity> getBills(long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
	{
		Map<String, Object> params = new HashMap<>();

		String query = "bill.car.user.userId = :userId";
		params.put("userId", username);

		if (carId >= 0)
		{
			query += " and bill.car.id = :carId";
			params.put("carId", carId);
		}

		if (from.isPresent() && to.isPresent())
		{
			query += " and bill.date >= :from and bill.date <= :to";
			params.put("from", from.get());
			params.put("to", to.get());

		} else if (from.isPresent())
		{
			query += " and bill.date >= :from";
			params.put("from", from.get());

		} else if (to.isPresent())
		{
			query += " and bill.date <= :to";
			params.put("to", to.get());
		}

		query += " order by bill.date desc";

		return find(query, params).list();
	}

	// public List<Integer> getBillYears(long carId, String username)
	// {
	// return find("select bill.date from FuelBillEntity where bill.car.id = ?1 and
	// bill.car.user.userId = ?2", carId,
	// username)
	// .project(LocalDate.class)
	// .list()
	// .stream()
	// .map(LocalDate::getYear)
	// .distinct()
	// .sorted(Comparator.reverseOrder())
	// .toList();
	// }

	@Override
	public boolean isPersistent(FuelBillEntity entity)
	{
		return find("bill.date = ?1 and unit = ?2 and bill.car.id = ?3 and distance = ?4",
				entity.getBill().getDate(),
				entity.getUnit(),
				entity.getBill().getCarId(),
				entity.getDistance())
				.firstResultOptional().isPresent();
	}

	@Override
	@Transactional
	public void delete(FuelBillEntity billEntity)
	{
		billRepository.delete(billEntity.getBill());
	}

	@Override
	@Transactional
	public void persist(FuelBillEntity billEntity)
	{
		super.persist(billEntity);
	}
}
