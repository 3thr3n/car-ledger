package de.codeflowwizardry.carledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import de.codeflowwizardry.carledger.data.repository.CarAccess;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.records.Odometer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Calculates the current odometer of cars. Readings are the odometer of the
 * car,
 * the ones of maintenance bills and the start and end of trips. The latest
 * reading is the base, the distances of the refuels or charges after its day
 * are added.
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
	 * Four queries, independent of the number of cars, bills and trips.
	 *
	 * @param carId
	 *            or {@link CarRepository#ALL_CARS}
	 * @return car id -> odometer, for every car of the user
	 */
	public Map<Long, Odometer> current(long carId, String username)
	{
		Map<Long, List<Reading>> readings = new HashMap<>();
		for (Object[] row : query(
				"select c.id, c.odometerDate, c.odometer from Car c where " + CarAccess.condition("c", Action.READ),
				"c", carId, username))
		{
			add(readings, row);
		}
		for (Object[] row : query("select b.car.id, b.date, m.odometer from MaintenanceBillEntity m join m.bill b"
				+ " where " + CarAccess.condition("b.car", Action.READ) + " and m.odometer is not null", "b.car", carId,
				username))
		{
			add(readings, row);
		}
		for (Object[] row : query("select t.car.id, t.startedAt, t.startOdometer, t.endedAt, t.endOdometer from trip t"
				+ " where " + CarAccess.condition("t.car", Action.READ), "t.car", carId, username))
		{
			add(readings, (Long) row[0], TripService.day((OffsetDateTime) row[1]), (Integer) row[2]);
			if (row[4] != null)
			{
				add(readings, (Long) row[0], TripService.day((OffsetDateTime) row[3]), (Integer) row[4]);
			}
		}

		Map<Long, NavigableMap<LocalDate, BigDecimal>> refueled = drivenPerDay("FuelBillEntity", carId, username);
		Map<Long, NavigableMap<LocalDate, BigDecimal>> charged = drivenPerDay("ChargingBillEntity", carId, username);

		Map<Long, Odometer> odometers = new HashMap<>();
		readings.forEach((id, carReadings) -> odometers.put(id,
				calculate(carReadings, refueled.getOrDefault(id, Collections.emptyNavigableMap()),
						charged.getOrDefault(id, Collections.emptyNavigableMap()))));
		return odometers;
	}

	/**
	 * @param entity
	 *            the refuels or the charges
	 * @return distance per car and day, one entry per day with a distance
	 */
	private Map<Long, NavigableMap<LocalDate, BigDecimal>> drivenPerDay(String entity, long carId, String username)
	{
		Map<Long, NavigableMap<LocalDate, BigDecimal>> driven = new HashMap<>();
		for (Object[] row : query("select b.car.id, b.date, sum(f.distance) from " + entity + " f join f.bill b"
				+ " where " + CarAccess.condition("b.car", Action.READ) + " and f.distance > 0", "b.car", carId,
				username,
				" group by b.car.id, b.date"))
		{
			driven.computeIfAbsent((Long) row[0], id -> new TreeMap<>()).put((LocalDate) row[1],
					(BigDecimal) row[2]);
		}
		return driven;
	}

	/**
	 * Both distances are the km since the last refuel or charge: a plug-in hybrid
	 * counts the larger one, not both.
	 */
	static Odometer calculate(List<Reading> readings, NavigableMap<LocalDate, BigDecimal> refueledPerDay,
			NavigableMap<LocalDate, BigDecimal> chargedPerDay)
	{
		// latest by day, the higher one on the same day
		Reading base = readings.stream()
				.max(Comparator.comparing(Reading::date).thenComparingInt(Reading::value))
				.orElseThrow();
		int highest = readings.stream().mapToInt(Reading::value).max().orElseThrow();

		int driven = Math.max(drivenSince(refueledPerDay, base.date()), drivenSince(chargedPerDay, base.date()));

		return new Odometer(Math.max(base.value() + driven, highest), base.value(), base.date(), driven);
	}

	private static int drivenSince(NavigableMap<LocalDate, BigDecimal> drivenPerDay, LocalDate day)
	{
		return drivenPerDay.tailMap(day, false)
				.values()
				.stream()
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.intValue();
	}

	private static void add(Map<Long, List<Reading>> readings, Object[] row)
	{
		add(readings, (Long) row[0], (LocalDate) row[1], (Integer) row[2]);
	}

	private static void add(Map<Long, List<Reading>> readings, long carId, LocalDate date, int value)
	{
		readings.computeIfAbsent(carId, id -> new ArrayList<>()).add(new Reading(date, value));
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
				.setParameter(CarAccess.USER, username);
		if (carId != CarRepository.ALL_CARS)
		{
			typedQuery.setParameter("carId", carId);
		}
		return typedQuery.getResultList();
	}
}
