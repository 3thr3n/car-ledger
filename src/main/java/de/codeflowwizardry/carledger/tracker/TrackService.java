package de.codeflowwizardry.carledger.tracker;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.PlaceEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.PlaceRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
import de.codeflowwizardry.carledger.data.repository.TripRepository;
import de.codeflowwizardry.carledger.data.TripEntity;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.tracker.DriveDetector.Drive;
import de.codeflowwizardry.carledger.rest.car.track.Track;
import de.codeflowwizardry.carledger.rest.car.track.TrackDrive;
import de.codeflowwizardry.carledger.rest.car.track.TrackFrequency;
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;
import de.codeflowwizardry.carledger.rest.car.track.TrackSegment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

/**
 * The GPS tracks of the cars, simplified for the map
 */
@ApplicationScoped
public class TrackService
{
	/** longest range of one query */
	public static final Duration MAX_RANGE = Duration.ofDays(31);
	/** a longer time without a point starts a new segment */
	public static final Duration GAP = Duration.ofMinutes(5);
	/** a point is kept where the speed changes by more than this, km/h */
	static final double SPEED_THRESHOLD = 10;
	/**
	 * meters, of the simplification: small, so the drives of a street stay on
	 * their shared line; the map simplifies further by its zoom
	 */
	static final double TOLERANCE = 1;

	private final CarRepository carRepository;
	private final TrackPointRepository trackPointRepository;
	private final TripRepository tripRepository;
	private final PlaceRepository placeRepository;
	private final TrackerConfig config;

	@Inject
	public TrackService(CarRepository carRepository, TrackPointRepository trackPointRepository,
			TripRepository tripRepository, PlaceRepository placeRepository, TrackerConfig config)
	{
		this.carRepository = carRepository;
		this.trackPointRepository = trackPointRepository;
		this.tripRepository = tripRepository;
		this.placeRepository = placeRepository;
		this.config = config;
	}

	/**
	 * Reads the points twice: for the shared line of the streets driven more than
	 * once, then segment by segment, only one segment of raw points is in memory
	 * at a time.
	 *
	 * @param to
	 *            exclusive
	 * @param selection
	 *            the drives to show, each a segment of its own
	 */
	@Transactional
	public Track track(long carId, OffsetDateTime from, OffsetDateTime to, Selection selection, String username)
	{
		Utils.requireValid(validate(from, to));
		if (carRepository.findById(carId, username, Action.READ) == null)
		{
			throw new WrongUserException();
		}

		SharedLine shared = new SharedLine(GAP);
		trackPointRepository.forEach(carId, from, to, selection.filter(shared::add, shared::cut));
		return collect(carId, from, to, selection, shared);
	}

	/**
	 * How often the car passed the roads of its track: reads the points twice, to
	 * count (and for the shared line) and then to draw the lines
	 *
	 * @param to
	 *            exclusive
	 * @param selection
	 *            the drives to count, the others don't add passes
	 */
	@Transactional
	public TrackFrequency frequency(long carId, OffsetDateTime from, OffsetDateTime to, Selection selection,
			String username)
	{
		Utils.requireValid(validate(from, to));
		if (carRepository.findById(carId, username, Action.READ) == null)
		{
			throw new WrongUserException();
		}

		SharedLine shared = new SharedLine(GAP);
		PassCounter counter = new PassCounter(GAP, TOLERANCE, shared);
		trackPointRepository.forEach(carId, from, to, selection.filter(point -> {
			counter.count(point);
			shared.add(point);
		}, () -> {
			counter.cut();
			shared.cut();
		}));
		trackPointRepository.forEach(carId, from, to, selection.filter(counter::trace, counter::cut));
		return counter.frequency();
	}

	/**
	 * The drives of the track, for picking some of them on the map: like the
	 * stops of the places, a drive ends where the car stood for longer than the
	 * stop, also with the tracker on. A drive already on the road at the start of
	 * the range starts there, one still on the road ends where it was last seen
	 * (as always, within the drift of the GPS).
	 *
	 * @param to
	 *            exclusive
	 * @return the newest first, with the names of the named places at their ends
	 */
	@Transactional
	public List<TrackDrive> drives(long carId, OffsetDateTime from, OffsetDateTime to, String username)
	{
		Utils.requireValid(validate(from, to));
		CarEntity car = carRepository.findById(carId, username, Action.READ);
		if (car == null)
		{
			throw new WrongUserException();
		}

		DriveDetector detector = new DriveDetector(config.stop(), Places.MIN_DRIVE_METERS, Places.MIN_DRIVE_DURATION,
				null);
		trackPointRepository.forEach(carId, from, to, detector::add);
		List<PlaceEntity> named = placeRepository.list(car);
		List<TrackDrive> drives = new ArrayList<>();
		for (Drive drive : detector.drives(OffsetDateTime.MAX))
		{
			drives.add(new TrackDrive(drive.start(), drive.end(), Math.round(drive.meters() / 10) / 100.0,
					name(named, drive.departure()), name(named, drive.arrival())));
		}
		return drives.reversed();
	}

	/**
	 * @return of the named place nearest to the point within
	 *         {@link PlaceFinder#RADIUS}, <code>null</code> without one
	 */
	private static String name(List<PlaceEntity> named, TrackPoint point)
	{
		return named.stream()
				.filter(place -> distance(place, point) <= PlaceFinder.RADIUS)
				.min(Comparator.comparingDouble(place -> distance(place, point)))
				.map(PlaceEntity::getName)
				.orElse(null);
	}

	private static double distance(PlaceEntity place, TrackPoint point)
	{
		return TrackSimplifier.distance(new TrackPoint(null, place.getLat(), place.getLon(), null), point);
	}

	/**
	 * The track during a trip, from its start to its end (now while it is open),
	 * also of a deleted trip. Only a hint next to the odometers: the trip log
	 * doesn't depend on it. A trip left open for longer is cut after
	 * {@link #MAX_RANGE}. The points stay where they are: a single drive has no
	 * shared line.
	 */
	@Transactional
	public Track tripTrack(long carId, UUID tripId, String username)
	{
		TripEntity trip = tripRepository.find(carId, tripId, username, Action.READ)
				.orElseThrow(() -> new NotFoundException("Trip not found"));
		OffsetDateTime from = trip.getStartedAt();
		OffsetDateTime end = trip.getEndedAt() != null ? trip.getEndedAt() : OffsetDateTime.now();
		// the end is part of the trip, the range excludes its end
		OffsetDateTime to = end.plusNanos(1_000);
		if (Duration.between(from, to).compareTo(MAX_RANGE) > 0)
		{
			to = from.plus(MAX_RANGE);
		}
		return collect(carId, from, to, Selection.ALL, null);
	}

	/**
	 * @param shared
	 *            of the points of the range, <code>null</code> for the raw
	 *            points
	 */
	private Track collect(long carId, OffsetDateTime from, OffsetDateTime to, Selection selection,
			SharedLine shared)
	{
		Collector collector = new Collector(TOLERANCE, shared);
		trackPointRepository.forEach(carId, from, to, selection.filter(collector::add, collector::finish));
		collector.finish();
		return new Track(collector.segments, Math.round(collector.meters / 10) / 100.0, collector.count);
	}

	static List<String> validate(OffsetDateTime from, OffsetDateTime to)
	{
		List<String> errors = new ArrayList<>();
		if (from == null || to == null)
		{
			errors.add("From and to must be set!");
		} else if (!to.isAfter(from))
		{
			errors.add("To must be after from!");
		} else if (Duration.between(from, to).compareTo(MAX_RANGE) > 0)
		{
			errors.add("The range must not be longer than " + MAX_RANGE.toDays() + " days!");
		}
		return errors;
	}

	/**
	 * Splits the points at the gaps, sums their distance and simplifies each
	 * finished segment, moved onto the shared line
	 */
	private static class Collector
	{
		private final double tolerance;
		private final SharedLine shared;
		private final List<TrackSegment> segments = new ArrayList<>();
		private List<TrackPoint> current = new ArrayList<>();
		private double meters;
		private int count;

		Collector(double tolerance, SharedLine shared)
		{
			this.tolerance = tolerance;
			this.shared = shared;
		}

		void add(TrackPoint point)
		{
			count++;
			if (!current.isEmpty())
			{
				TrackPoint previous = current.getLast();
				if (Duration.between(previous.ts(), point.ts()).compareTo(GAP) > 0)
				{
					finish();
				} else
				{
					meters += TrackSimplifier.distance(previous, point);
				}
			}
			current.add(point);
		}

		void finish()
		{
			if (!current.isEmpty())
			{
				List<TrackPoint> points = shared == null ? current : shared.apply(current);
				segments.add(TrackSegment.of(TrackSimplifier.simplify(points, tolerance, SPEED_THRESHOLD)));
				current = new ArrayList<>();
			}
		}
	}
}
