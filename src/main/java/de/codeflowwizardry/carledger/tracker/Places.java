package de.codeflowwizardry.carledger.tracker;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.PlaceEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.PlaceRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.car.track.Place;
import de.codeflowwizardry.carledger.rest.car.track.PlaceInput;
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;
import de.codeflowwizardry.carledger.tracker.DriveDetector.Drive;
import de.codeflowwizardry.carledger.tracker.PlaceFinder.Visits;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

/**
 * The places a car stops at often, found in the stops of its GPS track: every
 * drive ends with a visit of a place. The owner names them; the name is stored
 * with its position, so it stays when the track points are deleted, and a
 * named place shows even without visits.
 */
@ApplicationScoped
public class Places
{
	/** visits in the range a place needs to show without a name */
	static final int MIN_VISITS = 3;
	/**
	 * a drive to a place needs this many meters, less than a trip: the fuel
	 * station around the corner counts
	 */
	static final double MIN_DRIVE_METERS = 200;
	static final Duration MIN_DRIVE_DURATION = Duration.ofMinutes(1);

	private final CarRepository carRepository;
	private final TrackPointRepository trackPointRepository;
	private final PlaceRepository placeRepository;
	private final TrackerConfig config;

	@Inject
	public Places(CarRepository carRepository, TrackPointRepository trackPointRepository,
			PlaceRepository placeRepository, TrackerConfig config)
	{
		this.carRepository = carRepository;
		this.trackPointRepository = trackPointRepository;
		this.placeRepository = placeRepository;
		this.config = config;
	}

	/**
	 * @param to
	 *            exclusive
	 * @return the places with at least {@link #MIN_VISITS} visits in the range
	 *         and the named ones, the most visited first
	 */
	@Transactional
	public List<Place> list(long carId, OffsetDateTime from, OffsetDateTime to, String username)
	{
		Utils.requireValid(TrackService.validate(from, to));
		CarEntity car = car(carId, username, Action.READ);

		DriveDetector detector = new DriveDetector(config.stop(), MIN_DRIVE_METERS, MIN_DRIVE_DURATION, from);
		trackPointRepository.forEach(carId, from, to, detector::add);
		OffsetDateTime now = OffsetDateTime.now();
		PlaceFinder finder = new PlaceFinder();
		detector.drives(now.isBefore(to) ? now : to).stream()
				.map(Drive::arrival)
				.forEach(finder::add);

		return merge(finder.places(), placeRepository.list(car));
	}

	/**
	 * @return the found places with the names of the named ones near them, and
	 *         the named ones without visits
	 */
	static List<Place> merge(List<Visits> found, List<PlaceEntity> named)
	{
		List<Visits> left = new ArrayList<>(found);
		List<Place> places = new ArrayList<>();
		for (PlaceEntity name : named)
		{
			TrackPoint position = new TrackPoint(null, name.getLat(), name.getLon(), null);
			Optional<Visits> visits = left.stream()
					.filter(place -> TrackSimplifier.distance(place.point(), position) <= PlaceFinder.RADIUS)
					.min(Comparator.comparingDouble(place -> TrackSimplifier.distance(place.point(), position)));
			visits.ifPresent(left::remove);
			// where the owner named it, it doesn't move with the stops
			places.add(new Place(name.getId(), name.getName(), name.getLat(), name.getLon(),
					visits.map(Visits::count).orElse(0), visits.map(Visits::last).orElse(null)));
		}
		for (Visits visits : left)
		{
			if (visits.count() >= MIN_VISITS)
			{
				places.add(new Place(null, null, visits.lat(), visits.lon(), visits.count(), visits.last()));
			}
		}
		places.sort(Comparator.comparingInt(Place::visits).reversed());
		return places;
	}

	/**
	 * Names the place at the position, renames a named one within
	 * {@link PlaceFinder#RADIUS} of it
	 */
	@Transactional
	public void name(long carId, PlaceInput input, String username)
	{
		Utils.requireValid(input.validate(true));
		CarEntity car = car(carId, username, Action.WRITE);

		TrackPoint position = new TrackPoint(null, input.lat(), input.lon(), null);
		Optional<PlaceEntity> near = placeRepository.list(car).stream()
				.filter(place -> distance(place, position) <= PlaceFinder.RADIUS)
				.min(Comparator.comparingDouble(place -> distance(place, position)));
		if (near.isPresent())
		{
			near.get().setName(input.name().strip());
		} else
		{
			placeRepository.persist(new PlaceEntity(car, input.lat(), input.lon(), input.name().strip()));
		}
	}

	/**
	 * @throws NotFoundException
	 *             if the place does not belong to the car of the user
	 */
	@Transactional
	public void rename(long carId, long id, PlaceInput input, String username)
	{
		Utils.requireValid(input.validate(false));
		find(carId, id, username).setName(input.name().strip());
	}

	/**
	 * Removes the name, the place shows again only with enough visits
	 *
	 * @throws NotFoundException
	 *             if the place does not belong to the car of the user
	 */
	@Transactional
	public void delete(long carId, long id, String username)
	{
		placeRepository.delete(find(carId, id, username));
	}

	private PlaceEntity find(long carId, long id, String username)
	{
		return placeRepository.find(carId, id, username, Action.WRITE)
				.orElseThrow(() -> new NotFoundException("Place not found"));
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

	private static double distance(PlaceEntity place, TrackPoint position)
	{
		return TrackSimplifier.distance(new TrackPoint(null, place.getLat(), place.getLon(), null), position);
	}
}
