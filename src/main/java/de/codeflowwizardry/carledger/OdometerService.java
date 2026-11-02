package de.codeflowwizardry.carledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
 * car, the ones of maintenance bills and the start and end of trips. The latest
 * reading is the base, the distances of the refuels or charges after its day
 * are added.
 * <p>
 * The distance of a refuel is the one since the refuel before it, which may be
 * before the reading: only what was driven after the reading counts. The
 * odometer at the refuel before the reading comes from an earlier reading and
 * the distances up to it; without them, the part of the distance after the
 * reading by the days.
 */
@ApplicationScoped
public class OdometerService
{
	/** the average a day starts at a reading about this many days before */
	static final int RATE_DAYS = 90;
	/** the reading of the average is at least this many days before */
	static final int MIN_RATE_DAYS = 30;
	/** a car without data for longer is not driven further */
	static final int MAX_ESTIMATED_DAYS = 60;

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
	 * The refuels or charges of a day
	 *
	 * @param distance
	 *            the sum of their distances
	 * @param complete
	 *            each of them has a distance
	 */
	record Driven(BigDecimal distance, boolean complete)
	{
	}

	/**
	 * Four queries, independent of the number of cars, bills and trips.
	 *
	 * @param carId
	 *            or {@link CarRepository#ALL_CARS}
	 * @return car id -> odometer, for every car the user may use (also as
	 *         driver: only the odometer, not the entries)
	 */
	public Map<Long, Odometer> current(long carId, String username)
	{
		Map<Long, List<Reading>> readings = new HashMap<>();
		for (Object[] row : query(
				"select c.id, c.odometerDate, c.odometer from Car c where " + CarAccess.condition("c", Action.USE),
				"c", carId, username))
		{
			add(readings, row);
		}
		for (Object[] row : query("select b.car.id, b.date, m.odometer from MaintenanceBillEntity m join m.bill b"
				+ " where " + CarAccess.condition("b.car", Action.USE) + " and m.odometer is not null", "b.car", carId,
				username))
		{
			add(readings, row);
		}
		for (Object[] row : query("select t.car.id, t.startedAt, t.startOdometer, t.endedAt, t.endOdometer from trip t"
				+ " where t.deletedAt is null and " + CarAccess.condition("t.car", Action.USE), "t.car", carId,
				username))
		{
			add(readings, (Long) row[0], TripService.day((OffsetDateTime) row[1]), (Integer) row[2]);
			if (row[4] != null)
			{
				add(readings, (Long) row[0], TripService.day((OffsetDateTime) row[3]), (Integer) row[4]);
			}
		}

		Map<Long, NavigableMap<LocalDate, Driven>> refueled = drivenPerDay("FuelBillEntity", carId, username);
		Map<Long, NavigableMap<LocalDate, Driven>> charged = drivenPerDay("ChargingBillEntity", carId, username);

		Map<Long, Odometer> odometers = new HashMap<>();
		readings.forEach((id, carReadings) -> odometers.put(id,
				calculate(carReadings, refueled.getOrDefault(id, Collections.emptyNavigableMap()),
						charged.getOrDefault(id, Collections.emptyNavigableMap()), LocalDate.now())));
		return odometers;
	}

	/**
	 * @param entity
	 *            the refuels or the charges
	 * @return distance per car and day, one entry per day with a refuel or charge
	 */
	private Map<Long, NavigableMap<LocalDate, Driven>> drivenPerDay(String entity, long carId, String username)
	{
		Map<Long, NavigableMap<LocalDate, Driven>> driven = new HashMap<>();
		for (Object[] row : query("select b.car.id, b.date,"
				+ " sum(case when f.distance > 0 then f.distance else 0 end),"
				+ " sum(case when f.distance > 0 then 0 else 1 end)"
				+ " from " + entity + " f join f.bill b where " + CarAccess.condition("b.car", Action.USE),
				"b.car", carId, username, " group by b.car.id, b.date"))
		{
			driven.computeIfAbsent((Long) row[0], id -> new TreeMap<>())
					.put((LocalDate) row[1], new Driven((BigDecimal) row[2], ((Number) row[3]).longValue() == 0));
		}
		return driven;
	}

	/**
	 * Both distances are the km since the last refuel or charge: a plug-in hybrid
	 * counts the larger one, not both.
	 */
	static Odometer calculate(List<Reading> readings, NavigableMap<LocalDate, Driven> refueledPerDay,
			NavigableMap<LocalDate, Driven> chargedPerDay, LocalDate today)
	{
		// latest by day, the higher one on the same day
		Reading base = readings.stream()
				.max(Comparator.comparing(Reading::date).thenComparingInt(Reading::value))
				.orElseThrow();
		int highest = readings.stream().mapToInt(Reading::value).max().orElseThrow();

		int driven = Math.max(0,
				Math.max(drivenSince(readings, base, refueledPerDay), drivenSince(readings, base, chargedPerDay)));

		int value = Math.max(base.value() + driven, highest);

		// the day of the latest data: the reading or a refuel after it
		LocalDate known = base.date();
		for (NavigableMap<LocalDate, Driven> drivenPerDay : List.of(refueledPerDay, chargedPerDay))
		{
			NavigableMap<LocalDate, Driven> after = drivenPerDay.tailMap(base.date(), false);
			if (!after.isEmpty() && after.lastKey().isAfter(known))
			{
				known = after.lastKey();
			}
		}
		BigDecimal perDay = perDay(readings, value, known);
		Integer estimatedToday = null;
		if (perDay != null)
		{
			long days = Math.clamp(ChronoUnit.DAYS.between(known, today), 0, MAX_ESTIMATED_DAYS);
			estimatedToday = value + perDay.multiply(BigDecimal.valueOf(days))
					.setScale(0, RoundingMode.HALF_UP)
					.intValue();
		}

		return new Odometer(value, base.value(), base.date(), driven, perDay, estimatedToday);
	}

	/**
	 * The average km a day up to the latest data, from the reading closest to
	 * {@link #RATE_DAYS} before it and at least {@link #MIN_RATE_DAYS} before it
	 *
	 * @param value
	 *            the odometer on the day of the latest data
	 * @return with one decimal, none without such a reading
	 */
	private static BigDecimal perDay(List<Reading> readings, int value, LocalDate known)
	{
		LocalDate target = known.minusDays(RATE_DAYS);
		return readings.stream()
				.filter(reading -> !reading.date().isAfter(known.minusDays(MIN_RATE_DAYS)))
				.min(Comparator.comparingLong(reading -> Math.abs(ChronoUnit.DAYS.between(target, reading.date()))))
				.map(start -> BigDecimal.valueOf(Math.max(0, value - start.value()))
						.divide(BigDecimal.valueOf(ChronoUnit.DAYS.between(start.date(), known)), 1,
								RoundingMode.HALF_UP))
				.orElse(null);
	}

	/**
	 * The km driven after the base reading by the refuels (or charges) after its
	 * day. A reading on the day of a refuel includes it.
	 */
	private static int drivenSince(List<Reading> readings, Reading base, NavigableMap<LocalDate, Driven> drivenPerDay)
	{
		NavigableMap<LocalDate, Driven> after = drivenPerDay.tailMap(base.date(), false);
		if (after.isEmpty())
		{
			return 0;
		}
		BigDecimal sumAfter = sum(after);
		Map.Entry<LocalDate, Driven> previous = drivenPerDay.floorEntry(base.date());
		// the first one after the reading counts from the reading itself
		if (previous == null || previous.getKey().equals(base.date()))
		{
			return sumAfter.intValue();
		}

		// the odometer at the refuel before the reading, by an earlier reading and
		// the distances in between, all of them known
		Optional<Reading> anchor = readings.stream()
				.filter(reading -> !reading.date().isAfter(previous.getKey()))
				.max(Comparator.comparing(Reading::date).thenComparingInt(Reading::value));
		if (anchor.isPresent())
		{
			NavigableMap<LocalDate, Driven> between = drivenPerDay.subMap(anchor.get().date(), false,
					previous.getKey(), true);
			if (between.values().stream().allMatch(Driven::complete))
			{
				int atPrevious = anchor.get().value() + sum(between).intValue();
				return atPrevious + sumAfter.intValue() - base.value();
			}
		}

		// otherwise the part of the first distance after the reading by the days
		Map.Entry<LocalDate, Driven> first = after.firstEntry();
		long window = ChronoUnit.DAYS.between(previous.getKey(), first.getKey());
		long sinceReading = ChronoUnit.DAYS.between(base.date(), first.getKey());
		BigDecimal share = first.getValue()
				.distance()
				.multiply(BigDecimal.valueOf(sinceReading))
				.divide(BigDecimal.valueOf(window), 0, RoundingMode.HALF_UP);
		return sumAfter.subtract(first.getValue().distance()).add(share).intValue();
	}

	private static BigDecimal sum(NavigableMap<LocalDate, Driven> drivenPerDay)
	{
		return drivenPerDay.values().stream().map(Driven::distance).reduce(BigDecimal.ZERO, BigDecimal::add);
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
