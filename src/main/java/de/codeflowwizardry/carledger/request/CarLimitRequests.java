package de.codeflowwizardry.carledger.request;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.exception.AlreadyPresentException;
import de.codeflowwizardry.carledger.notification.Notifications;
import de.codeflowwizardry.carledger.rest.admin.MaxCarsInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Requests for more cars: a user who reached the limit asks for more, the
 * approval raises the limit of the account
 */
@ApplicationScoped
public class CarLimitRequests implements RequestHandler<Integer>
{
	/** a request is possible with at most this many free cars left */
	static final int FREE_CARS_TO_REQUEST = 1;

	private final RequestRepository repository;
	private final CarRepository carRepository;
	private final EntityManager em;
	private final Notifications notifications;

	@Inject
	public CarLimitRequests(RequestRepository repository, CarRepository carRepository, EntityManager em,
			Notifications notifications)
	{
		this.repository = repository;
		this.carRepository = carRepository;
		this.em = em;
		this.notifications = notifications;
	}

	@Override
	public RequestType type()
	{
		return RequestType.CAR_LIMIT;
	}

	/**
	 * @param cars
	 *            how many cars more
	 * @param reason
	 *            optional, shown to the administrator
	 * @throws jakarta.ws.rs.BadRequestException
	 *             if the input is invalid or the user can still add cars
	 * @throws AlreadyPresentException
	 *             if an open request exists
	 */
	@Transactional
	public RequestEntity request(AccountEntity account, Integer cars, String reason)
	{
		List<String> errors = new ArrayList<>();
		int maxMore = MaxCarsInput.LIMIT - account.getMaxCars();
		if (cars == null || cars < 1 || cars > maxMore)
		{
			errors.add(maxMore < 1
					? "The limit of " + MaxCarsInput.LIMIT + " cars is reached!"
					: "Request between 1 and " + maxMore + " cars!");
		}
		if (reason != null && reason.length() > RequestService.MAX_TEXT)
		{
			errors.add("The reason can have at most " + RequestService.MAX_TEXT + " characters!");
		}
		long free = account.getMaxCars() - carRepository.count("user", account);
		if (free > FREE_CARS_TO_REQUEST)
		{
			errors.add("You can still add " + free + " cars!");
		}
		Utils.requireValid(errors);

		if (repository.hasOpen(account, type()))
		{
			throw new AlreadyPresentException("You already requested more cars, wait for the decision!");
		}

		RequestEntity request = new RequestEntity(account, type(),
				new CarLimitPayload(cars, account.getMaxCars(), null).toMap(), StringUtils.trimToNull(reason));
		try
		{
			repository.persist(request);
			em.flush();
		}
		catch (ConstraintViolationException e)
		{
			// a second request at the same time, the index allows only one open request
			throw new AlreadyPresentException("You already requested more cars, wait for the decision!", e);
		}
		notifications.carRequestCreated(request, cars, account.getMaxCars());
		return request;
	}

	/**
	 * @param maxCars
	 *            the new limit, without one the current limit plus the requested
	 *            cars
	 */
	@Override
	public void approve(RequestEntity request, Integer maxCars)
	{
		AccountEntity account = request.getAccount();
		CarLimitPayload payload = CarLimitPayload.of(request.getPayload());
		int granted = maxCars == null ? account.getMaxCars() + payload.cars() : maxCars;
		if (granted <= account.getMaxCars() || granted > MaxCarsInput.LIMIT)
		{
			Utils.requireValid(List.of("The new limit must be above " + account.getMaxCars() + " and at most "
					+ MaxCarsInput.LIMIT + "!"));
		}
		account.setMaxCars(Math.min(granted, MaxCarsInput.LIMIT));
		request.setPayload(new CarLimitPayload(payload.cars(), payload.maxCars(), granted).toMap());
	}
}
