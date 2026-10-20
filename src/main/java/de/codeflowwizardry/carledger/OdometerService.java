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
 * <p>
 * After an odometer reset only the reset and the readings after it count: the
 * trips from its time on, the other readings from the day after it.
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
	 * the latest reset of a car, its odometer is a reading
	 */
	private record Reset(OffsetDateTime time, LocalDate day, int odometer)
	{
		boolean counts(LocalDate date)
		{
			return date.isAfter(day);
		}

		boolean counts(OffsetDateTime at)
		{
			return !at.isBefore(time);
		}
	}

	/**
	 * Six queries, independent of the number of cars, bills and trips.
	 *
	 * @param carId
	 *            or {@link CarRepository#ALL_CARS}
	 * @return car id -> odometer, for every car of the user
	 */
	public Map<Long, Odometer> current(long carId, String username)
	{
		Map<Long, List<Reading>> readings = new HashMap<>();
		Map<Long, Reset> resets = new HashMap<>();
		for (Object[] row : query("select r.car.id, r.resetAt, r.odometer from odometer_reset r"
				+ " where r.deletedAt is null and " + CarAccess.condition("r.car", Action.READ), "r.car", carId,
				username, " order by r.resetAt"))
		{
			// the latest one last
			OffsetDateTime time = (OffsetDateTime) row[1];
			resets.put((Long) row[0], new Reset(time, TripService.day(time), (Integer) row[2]));
		}
		for (Object[] row : query(
				"select c.id, c.odometerDate, c.odometer from Car c where " + CarAccess.condition("c", Action.READ),
				"c", carId, username))
		{
			addDay(readings, resets, row);
		}
		for (Object[] row : query("select b.car.id, b.date, m.odometer from MaintenanceBillEntity m join m.bill b"
				+ " where " + CarAccess.condition("b.car", Action.READ) + " and m.odometer is not null", "b.car", carId,
				username))
		{
			addDay(readings, resets, row);
		}
		for (Object[] row : query("select t.car.id, t.startedAt, t.startOdometer, t.endedAt, t.endOdometer from trip t"
				+ " where t.deletedAt is null and " + CarAccess.condition("t.car", Action.READ), "t.car", carId,
				username))
		{
			addTime(readings, resets, (Long) row[0], (OffsetDateTime) row[1], (Integer) row[2]);
			if (row[4] != null)
			{
				addTime(readings, resets, (Long) row[0], (OffsetDateTime) row[3], (Integer) row[4]);
			}
		}
		resets.forEach((id, reset) -> add(readings, id, reset.day(), reset.odometer()));

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

	/**
	 * A reading with a day, it counts after the day of a reset
	 */
	private static void addDay(Map<Long, List<Reading>> readings, Map<Long, Reset> resets, Object[] row)
	{
		Reset reset = resets.get((Long) row[0]);
		if (reset == null || reset.counts((LocalDate) row[1]))
		{
			add(readings, (Long) row[0], (LocalDate) row[1], (Integer) row[2]);
		}
	}

	/**
	 * A reading with a time, it counts from the time of a reset on
	 */
	private static void addTime(Map<Long, List<Reading>> readings, Map<Long, Reset> resets, long carId,
			OffsetDateTime time, int value)
	{
		Reset reset = resets.get(carId);
		if (reset == null || reset.counts(time))
		{
			add(readings, carId, TripService.day(time), value);
		}
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
