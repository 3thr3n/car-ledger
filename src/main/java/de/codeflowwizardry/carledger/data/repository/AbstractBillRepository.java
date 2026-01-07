package de.codeflowwizardry.carledger.data.repository;

import java.time.LocalDate;
import java.util.*;

import de.codeflowwizardry.carledger.data.AbstractBillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;

public abstract class AbstractBillRepository<T extends AbstractBillEntity> implements PanacheRepository<T>
{
	public PanacheQuery<T> getBills(long carId, String username, Page page, Optional<Integer> year)
	{
		String query = "bill.car.id = :carId and bill.car.user.userId = :username order by bill.date desc";
		Map<String, Object> params = new HashMap<>();
		params.put("carId", carId);
		params.put("username", username);

		if (year.isPresent())
		{
			query = "year(bill.date) = :year and " + query;
			params.put("year", year.get());
		}

		return find(query, params)
				.page(page);
	}

	public Optional<T> getBillById(long billId, long carId, String username)
	{
		return find("id = ?1 and bill.car.id = ?2 and bill.car.user.userId = ?3 order by bill.date desc",
				billId,
				carId,
				username)
				.firstResultOptional();
	}

	public List<Integer> getBillYears(long carId, String username, BillType type)
	{
		Map<String, Object> params = new HashMap<>();
		params.put("carId", carId);
		params.put("username", username);
		params.put("type", type);

		return find("bill.car.id = :carId and bill.car.user.userId = :username and bill.type = :type",
				params)
				.list()
				.stream()
				.map(t -> t.getBill().getDate())
				.map(LocalDate::getYear)
				.distinct()
				.sorted(Comparator.reverseOrder())
				.toList();
	}
}
