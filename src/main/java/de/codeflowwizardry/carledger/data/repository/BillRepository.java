package de.codeflowwizardry.carledger.data.repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.BillEntity;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class BillRepository extends AbstractBillRepository<BillEntity>
{
	public List<Integer> getBillYears(long carId, String username)
	{
		return find("select date from bill where car.id = :carId and car.user.userId = :userId",
				Parameters.with("carId", carId).and("userId", username))
				.project(LocalDate.class)
				.list()
				.stream()
				.map(LocalDate::getYear)
				.distinct()
				.sorted(Comparator.reverseOrder())
				.toList();
	}

	public PanacheQuery<BillEntity> findByCarAndYear(Long carId, Integer year, Page queryPage)
	{
		if (year == null)
		{
			return find("car.id = ?1 order by date desc", carId)
					.page(queryPage);
		}

		return find("car.id = ?1 and YEAR(date) = ?2 order by date desc", carId, year)
				.page(queryPage);
	}

	@Transactional
	public void delete(long carId, long billId, String username)
	{
		Optional<BillEntity> any = find("id = :billId and car.id = :carId and car.user.userId = :userId",
				Parameters.with("carId", carId).and("userId", username).and("billId", billId))
				.stream().findAny();

		if (any.isEmpty())
		{
			throw new RuntimeException("Bill could not be deleted (not found)");
		}

		delete(any.get());
	}
}
