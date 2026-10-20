package de.codeflowwizardry.carledger.data.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.OdometerResetEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The resets of a user are read through {@link CarAccess}. The methods with a
 * {@link CarEntity} expect a car the user may access already, and leave out
 * deleted resets: they don't count.
 */
@ApplicationScoped
public class OdometerResetRepository implements PanacheRepositoryBase<OdometerResetEntity, UUID>
{
	/**
	 * @return the resets of the car, the latest first, also the deleted ones
	 */
	public List<OdometerResetEntity> list(long carId, String username)
	{
		return list("from odometer_reset r where r.car.id = :carId and " + CarAccess.condition("r.car", Action.READ)
				+ " order by r.resetAt desc, r.id", Map.of("carId", carId, CarAccess.USER, username));
	}

	/**
	 * @return the reset, if the user may do the action with its car
	 */
	public Optional<OdometerResetEntity> find(long carId, UUID id, String username, Action action)
	{
		return find("from odometer_reset r where r.id = :id and r.car.id = :carId and "
				+ CarAccess.condition("r.car", action), Map.of("id", id, "carId", carId, CarAccess.USER, username))
				.firstResultOptional();
	}

	/**
	 * @return the latest reset at or before the time, the readings before it
	 *         don't count from then on
	 */
	public Optional<OdometerResetEntity> latestUntil(CarEntity car, OffsetDateTime time)
	{
		return find("car = ?1 and resetAt <= ?2 and deletedAt is null order by resetAt desc", car, time)
				.firstResultOptional();
	}

	/**
	 * @return the first reset after the time, the readings from it on don't count
	 *         before it
	 */
	public Optional<OdometerResetEntity> firstAfter(CarEntity car, OffsetDateTime time)
	{
		return find("car = ?1 and resetAt > ?2 and deletedAt is null order by resetAt", car, time)
				.firstResultOptional();
	}

	/**
	 * @return whether a reset is between the times, not at one of them: a trip
	 *         may end at the reset or start at it
	 */
	public boolean between(CarEntity car, OffsetDateTime start, OffsetDateTime end)
	{
		return count("car = ?1 and resetAt > ?2 and resetAt < ?3 and deletedAt is null", car, start, end) > 0;
	}
}
