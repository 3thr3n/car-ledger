package de.codeflowwizardry.carledger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarAccessEntity;
import de.codeflowwizardry.carledger.data.CarAccessState;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarAccessRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.exception.AlreadyPresentException;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.notification.Notifications;
import de.codeflowwizardry.carledger.rest.car.access.CarAccessEntry;
import de.codeflowwizardry.carledger.rest.car.access.CarAccessInput;
import de.codeflowwizardry.carledger.rest.car.access.CarInvitation;
import de.codeflowwizardry.carledger.rest.car.access.CarInvitationInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

/**
 * The owner invites other accounts to a car with a role and an optional
 * period, changes and revokes their access; the invited one accepts or
 * declines. An access counts only once accepted (see
 * {@link de.codeflowwizardry.carledger.data.repository.CarAccess}).
 */
@ApplicationScoped
public class CarAccessService
{
	private final CarAccessRepository carAccessRepository;
	private final CarRepository carRepository;
	private final AccountRepository accountRepository;
	private final Notifications notifications;

	@Inject
	public CarAccessService(CarAccessRepository carAccessRepository, CarRepository carRepository,
			AccountRepository accountRepository, Notifications notifications)
	{
		this.carAccessRepository = carAccessRepository;
		this.carRepository = carRepository;
		this.accountRepository = accountRepository;
		this.notifications = notifications;
	}

	/**
	 * @throws WrongUserException
	 *             if the user doesn't own the car
	 */
	public List<CarAccessEntry> list(long carId, String username)
	{
		ownedCar(carId, username);
		return carAccessRepository.listForCar(carId).stream().map(CarAccessEntry::convert).toList();
	}

	/**
	 * Invites the account with the user name or e-mail address, one that doesn't
	 * exist yet gets it with its first login. A declined invitation is asked
	 * again.
	 *
	 * @throws AlreadyPresentException
	 *             if the account has the access or the invitation already
	 */
	@Transactional
	public CarAccessEntry invite(long carId, CarInvitationInput input, String username)
	{
		Utils.requireValid(input.validate());
		CarEntity car = ownedCar(carId, username);
		String name = input.account().strip().toLowerCase(Locale.ROOT);
		Optional<AccountEntity> account = accountRepository.findByNameOrEmail(name);
		if (account.isPresent() && account.get().getId().equals(car.getUser().getId()))
		{
			throw new BadRequestException(Response.status(400).entity(List.of("You own the car!")).build());
		}

		Optional<CarAccessEntity> existing = account.isPresent()
				? carAccessRepository.findForCar(carId, account.get())
				: carAccessRepository.findForCar(carId, name);
		CarAccessEntity access;
		if (existing.isEmpty())
		{
			access = CarAccessEntity.invite(car, account.orElse(null), name, input.role(), car.getUser());
			carAccessRepository.persist(access);
		} else if (existing.get().getState() == CarAccessState.DECLINED)
		{
			access = existing.get();
			access.setState(CarAccessState.PENDING);
		} else
		{
			throw new AlreadyPresentException(existing.get().getState() == CarAccessState.PENDING
					? "The account is invited already!"
					: "The account has an access already!");
		}
		apply(access, input.access());
		carAccessRepository.flush();
		notifications.carAccessInvited(access);
		return CarAccessEntry.convert(access);
	}

	/**
	 * The owner changes the role or the period, of an invitation as well
	 */
	@Transactional
	public CarAccessEntry change(long carId, long id, CarAccessInput input, String username)
	{
		Utils.requireValid(input.validate());
		ownedCar(carId, username);
		CarAccessEntity access = findForCar(carId, id);
		apply(access, input);
		return CarAccessEntry.convert(access);
	}

	/**
	 * The owner ends the access early or takes back the invitation; the entries
	 * of the account stay with the car
	 */
	@Transactional
	public void revoke(long carId, long id, String username)
	{
		ownedCar(carId, username);
		CarAccessEntity access = findForCar(carId, id);
		carAccessRepository.delete(access);
	}

	/**
	 * @return the invitations the user hasn't answered yet
	 */
	public List<CarInvitation> invitations(String username)
	{
		return carAccessRepository.listPendingFor(username).stream().map(CarInvitation::convert).toList();
	}

	/**
	 * The invited user accepts or declines
	 *
	 * @throws NotFoundException
	 *             if the user has no such open invitation
	 */
	@Transactional
	public void answer(long id, boolean accept, String username)
	{
		CarAccessEntity access = carAccessRepository.findPendingFor(id, username)
				.orElseThrow(() -> new NotFoundException("Invitation not found for your user."));
		access.setState(accept ? CarAccessState.ACCEPTED : CarAccessState.DECLINED);
		notifications.carAccessAnswered(access);
	}

	/**
	 * The account takes the invitations made for it before it existed: by its
	 * user name, and by its e-mail address once the identity provider confirmed
	 * it
	 *
	 * @param email
	 *            the confirmed address, if there is one
	 */
	@Transactional
	public void claim(AccountEntity account, String email)
	{
		List<String> names = new ArrayList<>();
		names.add(account.getUserId().toLowerCase(Locale.ROOT));
		if (email != null)
		{
			names.add(email.toLowerCase(Locale.ROOT));
		}
		carAccessRepository.claim(account, names);
	}

	private CarEntity ownedCar(long carId, String username)
	{
		CarEntity car = carRepository.findById(carId, username, Action.MANAGE);
		if (car == null)
		{
			throw new WrongUserException();
		}
		return car;
	}

	private CarAccessEntity findForCar(long carId, long id)
	{
		return carAccessRepository.findForCar(carId, id)
				.orElseThrow(() -> new NotFoundException("Access not found for the car of your user."));
	}

	private static void apply(CarAccessEntity access, CarAccessInput input)
	{
		access.setRole(input.role());
		access.setValidFrom(input.validFrom());
		access.setValidUntil(input.validUntil());
	}
}
