package de.codeflowwizardry.carledger;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.OdometerResetEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.OdometerResetRepository;
import de.codeflowwizardry.carledger.data.repository.TripRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.car.trip.OdometerReset;
import de.codeflowwizardry.carledger.rest.car.trip.OdometerResetInput;
import de.codeflowwizardry.carledger.rest.car.trip.TripConflict.Reason;
import de.codeflowwizardry.carledger.rest.car.trip.TripDeleteInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

/**
 * Odometer resets of the trip log, by the owner of the car: a replaced
 * instrument cluster or a reset odometer sets a new base. The trips and
 * {@link OdometerService} count from it, see {@link TripService}. Append-only
 * like the trips: a wrong reset is deleted with a reason and stays listed; the
 * trips after it are not checked again then.
 */
@ApplicationScoped
public class OdometerResetService
{
	private final OdometerResetRepository resetRepository;
	private final TripRepository tripRepository;
	private final CarRepository carRepository;
	private final TripService tripService;
	private final EntityManager em;

	@Inject
	public OdometerResetService(OdometerResetRepository resetRepository, TripRepository tripRepository,
			CarRepository carRepository, TripService tripService, EntityManager em)
	{
		this.resetRepository = resetRepository;
		this.tripRepository = tripRepository;
		this.carRepository = carRepository;
		this.tripService = tripService;
		this.em = em;
	}

	/**
	 * @param created
	 *            <code>false</code> if the reset was added before with the same
	 *            values
	 */
	public record Added(OdometerReset reset, boolean created)
	{
	}

	public List<OdometerReset> list(long carId, String username)
	{
		car(carId, username, Action.READ);
		return resetRepository.list(carId, username).stream().map(OdometerReset::convert).toList();
	}

	/**
	 * Online only, like a live trip: what doesn't fit is rejected. Sent again with
	 * the same values, it answers with the reset.
	 */
	@Transactional
	public Added add(long carId, UUID id, OdometerResetInput input, String username)
	{
		List<String> errors = input.validate();
		if (errors.isEmpty() && TripService.inFuture(input.resetAt()))
		{
			errors.add("Time must not be in the future!");
		}
		Utils.requireValid(errors);

		// one after the other with the trips of the car
		CarEntity car = car(carId, username, Action.MANAGE);
		em.lock(car, LockModeType.PESSIMISTIC_WRITE);

		Optional<OdometerResetEntity> existing = resetRepository.findByIdOptional(id);
		if (existing.isPresent())
		{
			OdometerResetEntity reset = existing.get();
			if (!reset.getCar().getId().equals(car.getId()) || !reset.getResetAt().isEqual(input.resetAt())
					|| reset.getOdometer() != input.odometer()
					|| !Objects.equals(reset.getOdometerBefore(), input.odometerBefore()))
			{
				throw TripService.conflict(Reason.ID_IN_USE, "The id belongs to another reset!", null, null);
			}
			return new Added(OdometerReset.convert(reset), false);
		}
		if (!car.isTripLog())
		{
			Utils.requireValid(List.of("The trip log of the car is off, turn it on in the settings of the car!"));
		}

		if (tripRepository.onTheRoad(car, input.resetAt()))
		{
			throw TripService.conflict(Reason.OVERLAPS, "A trip of the car was on the road then!", null, null);
		}
		if (resetRepository.count("car = ?1 and resetAt = ?2 and deletedAt is null", car, input.resetAt()) > 0)
		{
			throw TripService.conflict(Reason.OVERLAPS, "Another reset of the car is at the same time!", null, null);
		}
		if (input.odometerBefore() != null)
		{
			int minimum = tripService.highestBefore(car, input.resetAt(), TripService.NONE);
			if (input.odometerBefore() < minimum)
			{
				throw TripService.conflict(Reason.ODOMETER_BELOW_PREVIOUS,
						"The odometer before must not be below " + minimum + " km!", minimum, null);
			}
		}
		Optional<Integer> maximum = tripService.lowestAfter(car, input.resetAt(), TripService.NONE);
		if (maximum.isPresent() && input.odometer() > maximum.get())
		{
			throw TripService.conflict(Reason.ODOMETER_ABOVE_NEXT,
					"The odometer must not be above " + maximum.get() + " km, a later reading!", null,
					maximum.get());
		}

		OdometerResetEntity reset = new OdometerResetEntity(id, car, input.resetAt(), input.odometer(),
				input.odometerBefore(), input.reason().strip(), username, false, OffsetDateTime.now());
		resetRepository.persist(reset);
		return new Added(OdometerReset.convert(reset), true);
	}

	/**
	 * Deletes a reset with a reason, it stays listed. Sent again, it answers with
	 * the deleted reset.
	 */
	@Transactional
	public OdometerReset delete(long carId, UUID id, TripDeleteInput input, String username)
	{
		Utils.requireValid(input.validate());
		OdometerResetEntity reset = resetRepository.find(car(carId, username, Action.MANAGE).getId(), id, username,
				Action.MANAGE)
				.orElseThrow(() -> new NotFoundException("Odometer reset not found"));
		if (!reset.isDeleted())
		{
			reset.delete(OffsetDateTime.now(), username, input.reason().strip());
		}
		return OdometerReset.convert(reset);
	}

	private CarEntity car(long carId, String username, Action action)
	{
		CarEntity car = carRepository.findById(carId, username, action);
		if (car == null)
		{
			throw new WrongUserException();
		}
		return car;
	}
}
