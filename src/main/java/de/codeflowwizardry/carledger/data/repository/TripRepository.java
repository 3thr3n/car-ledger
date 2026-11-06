package de.codeflowwizardry.carledger.data.repository;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.TripEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The trips of a user are read through {@link CarAccess}. The methods with a
 * {@link CarEntity} expect a car the user may access already.
 */
@ApplicationScoped
public class TripRepository implements PanacheRepositoryBase<TripEntity, UUID>
{
	/**
	 * @return the trips of the car, the latest first
	 */
	public PanacheQuery<TripEntity> page(long carId, String username, Optional<Integer> year, Page page)
	{
		Map<String, Object> params = new HashMap<>();
		String query = "from trip t where t.car.id = :carId and "
				+ CarAccess.own("t.car", "t.driver", Action.READ);
		params.put("carId", carId);
		params.put(CarAccess.USER, username);
		if (year.isPresent())
		{
			query += " and year(t.startedAt) = :year";
			params.put("year", year.get());
		}
		return find(query + " order by t.startedAt desc, t.id", params).page(page);
	}

	/**
	 * @param from
	 *            the trips started at or after it, <code>null</code> for all
	 * @param until
	 *            the trips started before it, <code>null</code> for all
	 * @return the trips of the car with their driver, the oldest first, also the
	 *         deleted ones
	 */
	public List<TripEntity> list(long carId, String username, OffsetDateTime from, OffsetDateTime until)
	{
		Map<String, Object> params = new HashMap<>();
		String query = "from trip t where t.car.id = :carId and "
				+ CarAccess.own("t.car", "t.driver", Action.READ);
		params.put("carId", carId);
		params.put(CarAccess.USER, username);
		if (from != null)
		{
			query += " and t.startedAt >= :from";
			params.put("from", from);
		}
		if (until != null)
		{
			query += " and t.startedAt < :until";
			params.put("until", until);
		}
		return list(query + " order by t.startedAt, t.id", params);
	}

	/**
	 * @return the years with trips, the latest first
	 */
	public List<Integer> years(long carId, String username)
	{
		return getEntityManager().createQuery("select distinct year(t.startedAt) from trip t where t.car.id = :carId"
				+ " and " + CarAccess.own("t.car", "t.driver", Action.READ) + " order by 1 desc", Integer.class)
				.setParameter("carId", carId)
				.setParameter(CarAccess.USER, username)
				.getResultList();
	}

	/**
	 * @return the trip, if the user may do the action with its car
	 */
	public Optional<TripEntity> find(long carId, UUID id, String username, Action action)
	{
		return find(
				"from trip t where t.id = :id and t.car.id = :carId and " + CarAccess.own("t.car", "t.driver", action),
				Map.of("id", id, "carId", carId, CarAccess.USER, username))
				.firstResultOptional();
	}

	public Optional<TripEntity> findOpen(CarEntity car)
	{
		return find("car = ?1 and endOdometer is null and deletedAt is null", car).firstResultOptional();
	}

	/**
	 * @return the latest end of the finished trips, not of deleted ones
	 */
	public Optional<OffsetDateTime> latestEnd(CarEntity car)
	{
		return Optional.ofNullable(getEntityManager().createQuery(
				"select max(t.endedAt) from trip t where t.car = :car and t.deletedAt is null", OffsetDateTime.class)
				.setParameter("car", car)
				.getSingleResult());
	}

	/**
	 * @return the highest odometer of the trips started before the time, the end
	 *         or the start of an open one; not of deleted trips, like the
	 *         methods below
	 */
	public Optional<Integer> highestBefore(CarEntity car, OffsetDateTime time, UUID exclude)
	{
		return Optional.ofNullable(getEntityManager().createQuery("select max(coalesce(t.endOdometer, t.startOdometer))"
				+ " from trip t where t.car = :car and t.startedAt < :time and t.id <> :exclude and t.deletedAt is null",
				Integer.class)
				.setParameter("car", car)
				.setParameter("time", time)
				.setParameter("exclude", exclude)
				.getSingleResult());
	}

	/**
	 * @return the lowest start of the trips started after the time
	 */
	public Optional<Integer> lowestAfter(CarEntity car, OffsetDateTime time, UUID exclude)
	{
		return Optional.ofNullable(getEntityManager().createQuery("select min(t.startOdometer)"
				+ " from trip t where t.car = :car and t.startedAt > :time and t.id <> :exclude and t.deletedAt is null",
				Integer.class)
				.setParameter("car", car)
				.setParameter("time", time)
				.setParameter("exclude", exclude)
				.getSingleResult());
	}

	/**
	 * @return the trips of the car on the road in the time, like
	 *         {@link #overlaps}, an open one until now
	 */
	public List<TripEntity> overlapping(CarEntity car, OffsetDateTime start, OffsetDateTime end)
	{
		return list("car = :car and deletedAt is null and startedAt < :end and (endedAt is null or endedAt > :start)",
				Map.of("car", car, "start", start, "end", end));
	}

	/**
	 * @param end
	 *            the same as the start to check only the start
	 * @return whether another trip of the car is on the road in the time, an open
	 *         one until now
	 */
	public boolean overlaps(CarEntity car, OffsetDateTime start, OffsetDateTime end, UUID exclude)
	{
		return count("car = :car and id <> :exclude and deletedAt is null and (startedAt < :end or startedAt = :start)"
				+ " and (endedAt is null or endedAt > :start)",
				Map.of("car", car, "exclude", exclude, "start", start, "end", end)) > 0;
	}
}
