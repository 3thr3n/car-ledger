package de.codeflowwizardry.carledger.data.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.RecurringBillEntity;
import de.codeflowwizardry.carledger.data.RecurringBillPaymentEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;

@ApplicationScoped
public class RecurringBillPaymentRepository implements PanacheRepository<RecurringBillPaymentEntity>
{
	public boolean exists(RecurringBillEntity bill, LocalDate dueDate)
	{
		return count("bill = ?1 and dueDate = ?2", bill, dueDate) > 0;
	}

	/**
	 * Sums all payments due in the given range. {@link CarRepository#ALL_CARS} sums
	 * over all cars
	 * of the user.
	 */
	public BigDecimal sumAmount(long carId, String username, Optional<LocalDate> from, Optional<LocalDate> to)
	{
		Map<String, Object> params = new HashMap<>();

		String query = "select sum(amount) from recurring_bill_payment where bill.car.user.userId = :userId";
		params.put("userId", username);

		if (carId != CarRepository.ALL_CARS)
		{
			query += " and bill.car.id = :carId";
			params.put("carId", carId);
		}

		if (from.isPresent())
		{
			query += " and dueDate >= :from";
			params.put("from", from.get());
		}

		if (to.isPresent())
		{
			query += " and dueDate <= :to";
			params.put("to", to.get());
		}

		TypedQuery<BigDecimal> typedQuery = getEntityManager().createQuery(query, BigDecimal.class);
		params.forEach(typedQuery::setParameter);

		BigDecimal sum = typedQuery.getSingleResult();
		return sum == null ? BigDecimal.ZERO : sum;
	}

	/**
	 * Removes the payments due on or after the given day
	 */
	public void deleteFrom(RecurringBillEntity bill, LocalDate day)
	{
		delete("bill = ?1 and dueDate >= ?2", bill, day);
	}

	public BigDecimal sumAmount(RecurringBillEntity bill)
	{
		BigDecimal sum = getEntityManager()
				.createQuery("select sum(amount) from recurring_bill_payment where bill = :bill", BigDecimal.class)
				.setParameter("bill", bill)
				.getSingleResult();
		return sum == null ? BigDecimal.ZERO : sum;
	}
}
