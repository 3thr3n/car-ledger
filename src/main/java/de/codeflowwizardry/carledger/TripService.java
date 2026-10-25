package de.codeflowwizardry.carledger;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.TripEntity;
import de.codeflowwizardry.carledger.data.TripReview;
import de.codeflowwizardry.carledger.data.TripType;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.TripRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.car.trip.Trip;
import de.codeflowwizardry.carledger.rest.car.trip.TripConflict;
import de.codeflowwizardry.carledger.rest.car.trip.TripConflict.Reason;
import de.codeflowwizardry.carledger.rest.car.trip.TripCurrent;
import de.codeflowwizardry.carledger.rest.car.trip.TripDeleteInput;
import de.codeflowwizardry.carledger.rest.car.trip.TripFinishInput;
import de.codeflowwizardry.carledger.rest.car.trip.TripPastInput;
import de.codeflowwizardry.carledger.rest.car.trip.TripStartInput;
import de.codeflowwizardry.carledger.rest.records.BillPaged;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

/**
 * The trip log, append-only: a trip is started and finished, then it can't be
 * changed. A wrong trip is deleted with a reason and stays listed; it doesn't
 * count for the checks and the odometer anymore.
 * <p>
 * Start and finish carry an id of the client, sent again they answer like the
 * first time. They are checked against the other data of the car in the order
 * the trips were driven, not in the order they arrived: a live request that
 * doesn't fit is rejected, one from the offline queue
 * ({@link TripStartInput#queued()}) is stored with a {@link TripReview}, so
 * nothing that waited on a phone is lost.
 */
@ApplicationScoped
public class TripService
{
	/** a time of the device may be ahead of the server by this */
	static final Duration CLOCK_TOLERANCE = Duration.ofMinutes(5);

	/** excludes no trip of the queries */
	public static final UUID NONE = new UUID(0, 0);

	private final TripRepository tripRepository;
	private final CarRepository carRepository;
	private final AccountRepository accountRepository;
	private final OdometerService odometerService;
	private final EntityManager em;

	@Inject
	public TripService(TripRepository tripRepository, CarRepository carRepository,
			AccountRepository accountRepository, OdometerService odometerService, EntityManager em)
	{
		this.tripRepository = tripRepository;
		this.carRepository = carRepository;
		this.accountRepository = accountRepository;
		this.odometerService = odometerService;
		this.em = em;
	}

	/**
	 * @param created
	 *            <code>false</code> if the trip was started before with the same
	 *            values
	 */
	public record Started(Trip trip, boolean created)
	{
	}

	public BillPaged<Trip> list(long carId, String username, Optional<Integer> year, int page, int size)
	{
		PanacheQuery<TripEntity> query = tripRepository.page(carId, username, year, new Page(page - 1, size));
		return new BillPaged<>(query.count(), page, size, query.list().stream().map(Trip::convert).toList());
	}

	public List<Integer> years(long carId, String username)
	{
		return tripRepository.years(carId, username);
	}

	public Trip get(long carId, UUID id, String username)
	{
		return Trip.convert(find(carId, id, username, Action.READ));
	}

	public TripCurrent current(long carId, String username)
	{
		CarEntity car = car(carId, username, Action.READ);
		Trip open = tripRepository.findOpen(car).map(Trip::convert).orElse(null);
		int estimated = odometerService.current(carId, username).get(carId).value();
		return new TripCurrent(open, highestBefore(car, OffsetDateTime.now(), NONE), estimated);
	}

	@Transactional
	public Started start(long carId, UUID id, TripStartInput input, String username)
	{
		List<String> errors = input.validate();
		if (input.startedAt() != null && inFuture(input.startedAt()))
		{
			errors.add("Start time must not be in the future!");
		}
		Utils.requireValid(errors);

		CarEntity car = lockedCar(carId, username);

		Optional<TripEntity> existing = tripRepository.findByIdOptional(id);
		if (existing.isEmpty() && !car.isTripLog())
		{
			Utils.requireValid(List.of("The trip log of the car is off, turn it on in the settings of the car!"));
		}
		if (existing.isPresent())
		{
			TripEntity trip = existing.get();
			if (!trip.getCar().getId().equals(car.getId()) || !sameStart(trip, input))
			{
				throw conflict(Reason.ID_IN_USE, "The id belongs to another trip!", null, null);
			}
			return new Started(Trip.convert(trip), false);
		}
		if (tripRepository.findOpen(car).isPresent())
		{
			throw conflict(Reason.OPEN_TRIP, "Another trip of the car is open, finish it first!", null, null);
		}

		TripReview review = null;
		int minimum = highestBefore(car, input.startedAt(), id);
		Optional<Integer> maximum = tripRepository.lowestAfter(car, input.startedAt(), id);
		if (input.startOdometer() < minimum)
		{
			review = check(input.queued(), TripReview.ODOMETER_BELOW_PREVIOUS,
					"The start must not be below " + minimum + " km!", minimum, null);
		} else if (maximum.isPresent() && input.startOdometer() > maximum.get())
		{
			review = check(input.queued(), TripReview.ODOMETER_ABOVE_NEXT,
					"The start must not be above " + maximum.get() + " km, the start of a later trip!", null,
					maximum.get());
		} else if (tripRepository.overlaps(car, input.startedAt(), input.startedAt(), id))
		{
			review = check(input.queued(), TripReview.OVERLAPS, "Another trip of the car was on the road then!", null,
					null);
		}

		TripEntity trip = new TripEntity(id, car, accountRepository.findByIdentifier(username), input.type(),
				input.startOdometer(), input.startedAt(), false, OffsetDateTime.now());
		trip.setDestination(clean(input.destination()));
		trip.setPurpose(clean(input.purpose()));
		trip.setBusinessPartner(clean(input.businessPartner()));
		trip.setReviewReason(review);
		tripRepository.persist(trip);
		return new Started(Trip.convert(trip), true);
	}

	@Transactional
	public Trip finish(long carId, UUID id, TripFinishInput input, String username)
	{
		List<String> errors = input.validate();
		if (input.endedAt() != null && inFuture(input.endedAt()))
		{
			errors.add("End time must not be in the future!");
		}
		Utils.requireValid(errors);

		CarEntity car = lockedCar(carId, username);
		TripEntity trip = find(car.getId(), id, username, Action.WRITE);

		if (!trip.isOpen())
		{
			if (sameFinish(trip, input))
			{
				return Trip.convert(trip);
			}
			throw conflict(Reason.ALREADY_FINISHED, "The trip was finished already!", null, null);
		}
		if (trip.isDeleted())
		{
			throw conflict(Reason.DELETED, "The trip was deleted!", null, null);
		}

		errors = new ArrayList<>();
		if (input.endOdometer() < trip.getStartOdometer())
		{
			errors.add("End odometer must not be below the start of " + trip.getStartOdometer() + " km!");
		}
		if (input.endedAt().isBefore(trip.getStartedAt()))
		{
			errors.add("End time must not be before the start!");
		}
		Utils.requireValid(errors);

		addDetail(trip.getDestination(), input.destination(), trip::setDestination);
		addDetail(trip.getPurpose(), input.purpose(), trip::setPurpose);
		addDetail(trip.getBusinessPartner(), input.businessPartner(), trip::setBusinessPartner);
		requireBusinessDetails(trip);

		TripReview review = null;
		Optional<Integer> maximum = tripRepository.lowestAfter(car, trip.getStartedAt(), id);
		if (maximum.isPresent() && input.endOdometer() > maximum.get())
		{
			review = check(input.queued(), TripReview.ODOMETER_ABOVE_NEXT,
					"The end must not be above " + maximum.get() + " km, the start of a later trip!", null,
					maximum.get());
		} else if (tripRepository.overlaps(car, trip.getStartedAt(), input.endedAt(), id))
		{
			review = check(input.queued(), TripReview.OVERLAPS, "Another trip of the car was on the road then!", null,
					null);
		}
		if (trip.getReviewReason() == null)
		{
			trip.setReviewReason(review);
		}
		trip.finish(input.endOdometer(), input.endedAt(), OffsetDateTime.now());
		return Trip.convert(trip);
	}

	/**
	 * @return the highest reading before the time: the odometer of the car,
	 *         maintenance bills and trips
	 */
	/**
	 * @return the lowest start of a trip at the time: the highest odometer known
	 *         before, of the car, a maintenance or another trip
	 */
	public int minimumOdometer(CarEntity car, OffsetDateTime time)
	{
		return highestBefore(car, time, NONE);
	}

	private int highestBefore(CarEntity car, OffsetDateTime time, UUID exclude)
	{
		LocalDate day = day(time);
		int highest = car.getOdometerDate() == null || !car.getOdometerDate().isAfter(day) ? car.getOdometer() : 0;
		Integer maintenance = em.createQuery("select max(m.odometer) from MaintenanceBillEntity m join m.bill b"
				+ " where b.car = :car and b.date <= :day", Integer.class)
				.setParameter("car", car)
				.setParameter("day", day)
				.getSingleResult();
		if (maintenance != null)
		{
			highest = Math.max(highest, maintenance);
		}
		return Math.max(highest, tripRepository.highestBefore(car, time, exclude).orElse(0));
	}

	/**
	 * @return the day of the time on the server, like the dates of the other
	 *         readings
	 */
	public static LocalDate day(OffsetDateTime time)
	{
		return time.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
	}

	private static boolean inFuture(OffsetDateTime time)
	{
		return time.isAfter(OffsetDateTime.now().plus(CLOCK_TOLERANCE));
	}

	/**
	 * @return the review for a queued request
	 * @throws ClientErrorException
	 *             409 for a live request
	 */
	private static TripReview check(boolean queued, TripReview review, String message, Integer minimum,
			Integer maximum)
	{
		if (!queued)
		{
			throw conflict(Reason.valueOf(review.name()), message, minimum, maximum);
		}
		return review;
	}

	private static ClientErrorException conflict(Reason reason, String message, Integer minimum, Integer maximum)
	{
		return new ClientErrorException(Response.status(Response.Status.CONFLICT)
				.entity(new TripConflict(reason, message, minimum, maximum))
				.build());
	}

	private static boolean sameStart(TripEntity trip, TripStartInput input)
	{
		return trip.getType() == input.type()
				&& trip.getStartOdometer() == input.startOdometer()
				&& trip.getStartedAt().isEqual(input.startedAt());
	}

	private static boolean sameFinish(TripEntity trip, TripFinishInput input)
	{
		return Objects.equals(trip.getEndOdometer(), input.endOdometer())
				&& trip.getEndedAt().isEqual(input.endedAt())
				&& sameOrMissing(trip.getDestination(), input.destination())
				&& sameOrMissing(trip.getPurpose(), input.purpose())
				&& sameOrMissing(trip.getBusinessPartner(), input.businessPartner());
	}

	private static boolean sameOrMissing(String stored, String given)
	{
		return clean(given) == null || clean(given).equals(stored);
	}

	/**
	 * Sets a detail that is missing; one given at the start can be repeated, but
	 * not changed
	 */
	private static void addDetail(String stored, String given, Consumer<String> setter)
	{
		String value = clean(given);
		if (value == null)
		{
			return;
		}
		if (stored != null && !stored.equals(value))
		{
			throw conflict(Reason.DETAIL_CHANGED, "A detail given at the start can't be changed!", null, null);
		}
		setter.accept(value);
	}

	private static String clean(String value)
	{
		return value == null || value.isBlank() ? null : value.strip();
	}

	/**
	 * Deletes a trip with a reason, the owner of the car or an editor, not a
	 * driver; the trip stays listed with the reason. Sent again, it answers with
	 * the deleted trip.
	 */
	@Transactional
	public Trip delete(long carId, UUID id, TripDeleteInput input, String username)
	{
		Utils.requireValid(input.validate());
		TripEntity trip = find(car(carId, username, Action.WRITE).getId(), id, username, Action.WRITE);
		if (!trip.isDeleted())
		{
			trip.delete(OffsetDateTime.now(), username, input.reason().strip());
		}
		return Trip.convert(trip);
	}

	/**
	 * A forgotten trip entered afterwards, started and finished at once. Unlike a
	 * start it may come while another trip is open, if it doesn't overlap it. It
	 * is checked like a live request: what doesn't fit is rejected. Sent again
	 * with the same values, it answers with the trip.
	 */
	@Transactional
	public Started addPast(long carId, UUID id, TripPastInput input, String username)
	{
		List<String> errors = input.validate();
		if (errors.isEmpty() && inFuture(input.endedAt()))
		{
			errors.add("End time must not be in the future!");
		}
		Utils.requireValid(errors);

		CarEntity car = lockedCar(carId, username);
		Optional<TripEntity> existing = tripRepository.findByIdOptional(id);
		if (existing.isPresent())
		{
			TripEntity trip = existing.get();
			if (!trip.getCar().getId().equals(car.getId()) || trip.isOpen() || !sameStart(trip, input.start())
					|| !sameFinish(trip, input.finish()))
			{
				throw conflict(Reason.ID_IN_USE, "The id belongs to another trip!", null, null);
			}
			return new Started(Trip.convert(trip), false);
		}
		if (!car.isTripLog())
		{
			Utils.requireValid(List.of("The trip log of the car is off, turn it on in the settings of the car!"));
		}

		int minimum = highestBefore(car, input.startedAt(), id);
		Optional<Integer> maximum = tripRepository.lowestAfter(car, input.startedAt(), id);
		if (input.startOdometer() < minimum)
		{
			throw conflict(Reason.ODOMETER_BELOW_PREVIOUS, "The start must not be below " + minimum + " km!", minimum,
					null);
		}
		if (maximum.isPresent() && input.endOdometer() > maximum.get())
		{
			throw conflict(Reason.ODOMETER_ABOVE_NEXT,
					"The end must not be above " + maximum.get() + " km, the start of a later trip!", null,
					maximum.get());
		}
		if (tripRepository.overlaps(car, input.startedAt(), input.endedAt(), id))
		{
			throw conflict(Reason.OVERLAPS, "Another trip of the car was on the road then!", null, null);
		}

		OffsetDateTime now = OffsetDateTime.now();
		TripEntity trip = new TripEntity(id, car, accountRepository.findByIdentifier(username), input.type(),
				input.startOdometer(), input.startedAt(), false, now);
		trip.setDestination(clean(input.destination()));
		trip.setPurpose(clean(input.purpose()));
		trip.setBusinessPartner(clean(input.businessPartner()));
		requireBusinessDetails(trip);
		trip.finish(input.endOdometer(), input.endedAt(), now);
		tripRepository.persist(trip);
		return new Started(Trip.convert(trip), true);
	}

	private static void requireBusinessDetails(TripEntity trip)
	{
		if (trip.getType() == TripType.BUSINESS && (trip.getDestination() == null || trip.getPurpose() == null
				|| trip.getBusinessPartner() == null))
		{
			Utils.requireValid(List.of("A business trip needs a destination, a purpose and a business partner!"));
		}
	}

	private TripEntity find(long carId, UUID id, String username, Action action)
	{
		return tripRepository.find(carId, id, username, action)
				.orElseThrow(() -> new NotFoundException("Trip not found"));
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

	/**
	 * Starts and finishes of a car one after the other, so two at the same time
	 * see each other
	 */
	private CarEntity lockedCar(long carId, String username)
	{
		CarEntity car = car(carId, username, Action.WRITE);
		em.lock(car, LockModeType.PESSIMISTIC_WRITE);
		return car;
	}
}
