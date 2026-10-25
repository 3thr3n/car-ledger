package de.codeflowwizardry.carledger.data.repository;

import java.util.Map;
import java.util.stream.Collectors;

import de.codeflowwizardry.carledger.data.CarAccessEntity;
import de.codeflowwizardry.carledger.data.CarRole;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CarAccessRepository implements PanacheRepository<CarAccessEntity>
{
	/**
	 * @return the role of the user per car shared with them now, the own cars
	 *         are missing
	 */
	public Map<Long, CarRole> rolesOf(String userId)
	{
		return getEntityManager()
				.createQuery("select a.car.id, a.role from car_access a where a.account.userId = :user"
						+ " and (a.validFrom is null or a.validFrom <= current_timestamp)"
						+ " and (a.validUntil is null or a.validUntil > current_timestamp)", Object[].class)
				.setParameter("user", userId)
				.getResultStream()
				.collect(Collectors.toMap(row -> (Long) row[0], row -> (CarRole) row[1]));
	}
}
