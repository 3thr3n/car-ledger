package de.codeflowwizardry.carledger.data.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.DeviceEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeviceRepository implements PanacheRepository<DeviceEntity>
{
	/**
	 * @return the trackers of the car, if the user may manage it, the newest first
	 */
	public List<DeviceEntity> list(long carId, String username)
	{
		return list(
				"select d from device d join d.car c where c.id = :carId and " + CarAccess.condition("c", Action.MANAGE)
						+ " order by d.id desc",
				Map.of("carId", carId, CarAccess.USER, username));
	}

	/**
	 * @return the tracker, if the user may manage its car
	 */
	public Optional<DeviceEntity> find(long carId, long id, String username)
	{
		return find("select d from device d join d.car c where d.id = :id and c.id = :carId and "
				+ CarAccess.condition("c", Action.MANAGE),
				Map.of("id", id, "carId", carId, CarAccess.USER, username))
				.firstResultOptional();
	}

	public boolean existsByTokenHash(String tokenHash)
	{
		return count("tokenHash", tokenHash) > 0;
	}

	/**
	 * @return the tracker with the token, if it isn't revoked
	 */
	public Optional<DeviceEntity> findActiveByTokenHash(String tokenHash)
	{
		return find("from device d join fetch d.car where d.tokenHash = ?1 and d.revokedAt is null", tokenHash)
				.firstResultOptional();
	}

	/**
	 * Sets the last contact of the tracker, unless it was set after
	 * <code>notAfter</code> already: a tracker sends every few seconds, the row is
	 * only written once in a while.
	 */
	public void touch(long id, OffsetDateTime now, OffsetDateTime notAfter)
	{
		update("lastSeenAt = :now where id = :id and (lastSeenAt is null or lastSeenAt < :notAfter)",
				Map.of("now", now, "id", id, "notAfter", notAfter));
	}
}
