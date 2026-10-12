package de.codeflowwizardry.carledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.records.Odometer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Calculates the current odometer of cars. Readings are the odometer of the car
 * and the ones of maintenance bills. The latest reading is the base, the
 * distances of the refuels after its day are added.
 */
@ApplicationScoped
public class OdometerService
{
	private final EntityManager entityManager;

	@Inject
	public OdometerService(EntityManager entityManager)
	{
		this.entityManager = entityManager;
	}

	private record Reading(LocalDate date, int value)
	{
	}

	/**
	 * Three queries, independent of the number of cars and bills.
	 *
	 * @param carId
	 *            or {@link CarRepository#ALL_CARS}
	 * @return car id -> odometer, for every car of the user
	 */
	public Map<Long, Odometer> current(long carId, String username)
	{
		Map<Long, List<Reading>> readings = new HashMap<>();
		for (Object[] row : query("select c.id, c.odometerDate, c.odometer from Car c where c.user.userId = :userId",
				"c", carId, username))
		{
			add(readings, row);
		}
		for (Object[] row : query("select b.car.id, b.date, m.odometer from MaintenanceBillEntity m join m.bill b"
				+ " where b.car.user.userId = :userId and m.odometer is not null", "b.car", carId, username))
		{
			add(readings, row);
		}

		// refuel distance per car and day, one row per day with a refuel
		Map<Long, NavigableMap<LocalDate, BigDecimal>> driven = new HashMap<>();
		for (Object[] row : query("select b.car.id, b.date, sum(f.distance) from FuelBillEntity f join f.bill b"
				+ " where b.car.user.userId = :userId and f.distance > 0", "b.car", carId, username,
				" group by b.car.id, b.date"))
		{
			driven.computeIfAbsent((Long) row[0], id -> new TreeMap<>()).put((LocalDate) row[1],
					(BigDecimal) row[2]);
		}

		Map<Long, Odometer> odometers = new HashMap<>();
		readings.forEach((id, carReadings) -> odometers.put(id,
				calculate(carReadings, driven.getOrDefault(id, Collections.emptyNavigableMap()))));
		return odometers;
	}

	static Odometer calculate(List<Reading> readings, NavigableMap<LocalDate, BigDecimal> drivenPerDay)
	{
		// latest by day, the higher one on the same day
		Reading base = readings.stream()
				.max(Comparator.comparing(Reading::date).thenComparingInt(Reading::value))
				.orElseThrow();
		int highest = readings.stream().mapToInt(Reading::value).max().orElseThrow();

		int driven = drivenPerDay.tailMap(base.date(), false)
				.values()
				.stream()
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.intValue();

		return new Odometer(Math.max(base.value() + driven, highest), base.value(), base.date(), driven);
	}

	private static void add(Map<Long, List<Reading>> readings, Object[] row)
	{
		readings.computeIfAbsent((Long) row[0], id -> new ArrayList<>())
				.add(new Reading((LocalDate) row[1], (Integer) row[2]));
	}

	private List<Object[]> query(String query, String carPath, long carId, String username)
	{
		return query(query, carPath, carId, username, "");
	}

	private List<Object[]> query(String query, String carPath, long carId, String username, String suffix)
	{
		if (carId != CarRepository.ALL_CARS)
		{
			query += " and " + carPath + ".id = :carId";
		}
		TypedQuery<Object[]> typedQuery = entityManager.createQuery(query + suffix, Object[].class)
				.setParameter("userId", username);
		if (carId != CarRepository.ALL_CARS)
		{
			typedQuery.setParameter("carId", carId);
		}
		return typedQuery.getResultList();
	}
}
