package de.codeflowwizardry.carledger.data.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarAccessEntity;
import de.codeflowwizardry.carledger.data.CarAccessState;
import de.codeflowwizardry.carledger.data.CarRole;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
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
						+ " and a.state = :accepted"
						+ " and (a.validFrom is null or a.validFrom <= current_timestamp)"
						+ " and (a.validUntil is null or a.validUntil > current_timestamp)", Object[].class)
				.setParameter("user", userId)
				.setParameter("accepted", CarAccessState.ACCEPTED)
				.getResultStream()
				.collect(Collectors.toMap(row -> (Long) row[0], row -> (CarRole) row[1]));
	}

	/**
	 * @return the accounts that edit the car now, e.g. for its due reminders
	 */
	public List<AccountEntity> editorsOf(long carId)
	{
		return getEntityManager()
				.createQuery("select a.account from car_access a where a.car.id = :car and a.role = :editor"
						+ " and a.state = :accepted"
						+ " and (a.validFrom is null or a.validFrom <= current_timestamp)"
						+ " and (a.validUntil is null or a.validUntil > current_timestamp)", AccountEntity.class)
				.setParameter("car", carId)
				.setParameter("editor", CarRole.EDITOR)
				.setParameter("accepted", CarAccessState.ACCEPTED)
				.getResultList();
	}

	/**
	 * @return the accesses and invitations of the car, the oldest first
	 */
	public List<CarAccessEntity> listForCar(long carId)
	{
		return list("car.id", Sort.by("createdAt").and("id"), carId);
	}

	public Optional<CarAccessEntity> findForCar(long carId, long id)
	{
		return find("id = ?1 and car.id = ?2", id, carId).firstResultOptional();
	}

	/**
	 * @return the access of the account to the car, in any state
	 */
	public Optional<CarAccessEntity> findForCar(long carId, AccountEntity account)
	{
		return find("car.id = ?1 and account = ?2", carId, account).firstResultOptional();
	}

	/**
	 * @return the invitation of an account that doesn't exist yet
	 */
	public Optional<CarAccessEntity> findForCar(long carId, String invitee)
	{
		return find("car.id = ?1 and account is null and invitee = ?2", carId, invitee).firstResultOptional();
	}

	/**
	 * @return the invitations the user hasn't answered yet, the oldest first
	 */
	public List<CarAccessEntity> listPendingFor(String userId)
	{
		return list("account.userId = ?1 and state = ?2", Sort.by("createdAt").and("id"), userId,
				CarAccessState.PENDING);
	}

	public Optional<CarAccessEntity> findPendingFor(long id, String userId)
	{
		return find("id = ?1 and account.userId = ?2 and state = ?3", id, userId, CarAccessState.PENDING)
				.firstResultOptional();
	}

	/**
	 * The account takes the invitations made for its names before it existed;
	 * one for a car it already has an access to is dropped, the older access
	 * stays.
	 *
	 * @param names
	 *            in lower case: the user name and the confirmed e-mail address
	 * @return the claimed invitations
	 */
	public List<CarAccessEntity> claim(AccountEntity account, Collection<String> names)
	{
		List<CarAccessEntity> claimed = new ArrayList<>();
		// e.g. invited by the user name and by the address to the same car
		Set<Long> cars = new HashSet<>();
		for (CarAccessEntity invitation : list("account is null and invitee in ?1", Sort.by("createdAt").and("id"),
				names))
		{
			long carId = invitation.getCar().getId();
			if (!cars.add(carId) || invitation.getCar().getUser().getId().equals(account.getId())
					|| findForCar(carId, account).isPresent())
			{
				delete(invitation);
			} else
			{
				invitation.claim(account);
				claimed.add(invitation);
			}
		}
		return claimed;
	}
}
