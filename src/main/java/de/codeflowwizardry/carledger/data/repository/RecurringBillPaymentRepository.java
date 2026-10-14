package de.codeflowwizardry.carledger.data.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
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

		String query = "select sum(amount) from recurring_bill_payment"
				+ PeriodFilter.where("bill.car", "dueDate", carId, username, from, to, params);

		TypedQuery<BigDecimal> typedQuery = getEntityManager().createQuery(query, BigDecimal.class);
		params.forEach(typedQuery::setParameter);

		BigDecimal sum = typedQuery.getSingleResult();
		return sum == null ? BigDecimal.ZERO : sum;
	}

	/**
	 * Sums the payments per month they are due in, months without payments are
	 * missing. {@link CarRepository#ALL_CARS} sums over all cars of the user.
	 */
	public Map<YearMonth, BigDecimal> sumAmountPerMonth(long carId, String username, Optional<LocalDate> from,
			Optional<LocalDate> to)
	{
		Map<String, Object> params = new HashMap<>();

		String query = "select year(dueDate), month(dueDate), sum(amount) from recurring_bill_payment"
				+ PeriodFilter.where("bill.car", "dueDate", carId, username, from, to, params);

		TypedQuery<Object[]> typedQuery = getEntityManager()
				.createQuery(query + " group by year(dueDate), month(dueDate)", Object[].class);
		params.forEach(typedQuery::setParameter);

		Map<YearMonth, BigDecimal> sums = new HashMap<>();
		List<Object[]> rows = typedQuery.getResultList();
		rows.forEach(row -> sums.put(YearMonth.of((Integer) row[0], (Integer) row[1]), (BigDecimal) row[2]));
		return sums;
	}

	/**
	 * Removes the payments due on or after the given day
	 */
	public void deleteFrom(RecurringBillEntity bill, LocalDate day)
	{
		delete("bill = ?1 and dueDate >= ?2", bill, day);
	}

	/**
	 * Removes all payments of the bill
	 */
	public void deleteAll(RecurringBillEntity bill)
	{
		delete("bill", bill);
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
