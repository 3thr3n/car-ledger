package de.codeflowwizardry.carledger.tracker;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import de.codeflowwizardry.carledger.TripService;
import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.TripSuggestionDismissalEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
import de.codeflowwizardry.carledger.data.repository.TripRepository;
import de.codeflowwizardry.carledger.data.repository.TripSuggestionDismissalRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.car.trip.TripSuggestion;
import de.codeflowwizardry.carledger.rest.car.trip.TripSuggestionDismissInput;
import de.codeflowwizardry.carledger.tracker.DriveDetector.Drive;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Drives of the GPS tracks without a trip, suggested as forgotten trips. GPS
 * never writes a trip: the user adds a suggestion as a past trip or dismisses
 * it.
 */
@ApplicationScoped
public class TripSuggestions
{
	/** the drives of at most this time back are suggested */
	public static final Duration WINDOW = Duration.ofDays(7);

	private final CarRepository carRepository;
	private final TripRepository tripRepository;
	private final TrackPointRepository trackPointRepository;
	private final TripSuggestionDismissalRepository dismissalRepository;
	private final TripService tripService;
	private final TrackerConfig config;

	@Inject
	public TripSuggestions(CarRepository carRepository, TripRepository tripRepository,
			TrackPointRepository trackPointRepository, TripSuggestionDismissalRepository dismissalRepository,
			TripService tripService, TrackerConfig config)
	{
		this.carRepository = carRepository;
		this.tripRepository = tripRepository;
		this.trackPointRepository = trackPointRepository;
		this.dismissalRepository = dismissalRepository;
		this.tripService = tripService;
		this.config = config;
	}

	/**
	 * The drives since the end of the last trip, of at most {@link #WINDOW}, the
	 * oldest first. The odometers follow each other: a suggestion starts where
	 * the one before ends, or at the highest odometer known before it (the one
	 * of the car without any).
	 */
	@Transactional
	public List<TripSuggestion> list(long carId, String username)
	{
		CarEntity car = car(carId, username);
		if (!car.isTripLog())
		{
			return List.of();
		}
		OffsetDateTime now = OffsetDateTime.now();
		OffsetDateTime from = now.minus(WINDOW);
		OffsetDateTime lastEnd = tripRepository.latestEnd(car).orElse(from);
		if (lastEnd.isAfter(from))
		{
			from = lastEnd;
		}

		DriveDetector detector = new DriveDetector(config.stop(), config.minDriveDistance(),
				config.minDriveDuration(), from);
		trackPointRepository.forEach(car.getId(), from, now, detector::add);
		List<TripSuggestionDismissalEntity> dismissed = dismissalRepository.endingAfter(car, from);

		List<TripSuggestion> suggestions = new ArrayList<>();
		int odometer = 0;
		for (Drive drive : detector.drives(now))
		{
			if (tripRepository.overlaps(car, drive.start(), drive.end(), TripService.NONE)
					|| dismissed.stream().anyMatch(d -> d.getStartedAt().isBefore(drive.end())
							&& d.getEndedAt().isAfter(drive.start())))
			{
				continue;
			}
			double km = Math.round(drive.meters() / 10) / 100.0;
			int known = tripService.minimumOdometer(car, drive.start());
			// no reading before the drive, e.g. a car entered after it: the one of the car
			int start = Math.max(odometer, known > 0 ? known : car.getOdometer());
			odometer = start + (int) Math.round(km);
			suggestions.add(new TripSuggestion(drive.start(), drive.end(), km, start, odometer));
		}
		return suggestions;
	}

	/**
	 * A drive of the track as a trip to add, e.g. one picked on the map: the
	 * odometers like a suggestion, from the highest one known before it. Also for
	 * a drive that was dismissed or is older than {@link #WINDOW}.
	 *
	 * @param end
	 *            inclusive, the last point of the drive
	 */
	@Transactional
	public TripSuggestion forDrive(long carId, OffsetDateTime start, OffsetDateTime end, String username)
	{
		List<String> errors = new ArrayList<>();
		if (start == null || end == null)
		{
			errors.add("Start and end time must be set!");
		} else if (!end.isAfter(start))
		{
			errors.add("End time must be after the start time!");
		} else if (Duration.between(start, end).compareTo(TrackService.MAX_RANGE) > 0)
		{
			errors.add("The drive must not be longer than " + TrackService.MAX_RANGE.toDays() + " days!");
		}
		Utils.requireValid(errors);
		CarEntity car = car(carId, username);

		// the same drive as in the list of the map
		DriveDetector detector = new DriveDetector(config.stop(), Places.MIN_DRIVE_METERS, Places.MIN_DRIVE_DURATION,
				null);
		trackPointRepository.forEach(car.getId(), start, end.plus(1, ChronoUnit.MICROS), detector::add);
		double meters = detector.drives(OffsetDateTime.MAX).stream().mapToDouble(Drive::meters).sum();
		double km = Math.round(meters / 10) / 100.0;
		int known = tripService.minimumOdometer(car, start);
		int startOdometer = known > 0 ? known : car.getOdometer();
		return new TripSuggestion(start, end, km, startOdometer, startOdometer + (int) Math.round(km));
	}

	/**
	 * The drive isn't suggested anymore, also when it is found a little
	 * differently later
	 */
	@Transactional
	public void dismiss(long carId, TripSuggestionDismissInput input, String username)
	{
		Utils.requireValid(input.validate());
		dismissalRepository.persist(
				new TripSuggestionDismissalEntity(car(carId, username), input.startedAt(), input.endedAt()));
	}

	/** who may add trips to the car */
	private CarEntity car(long carId, String username)
	{
		CarEntity car = carRepository.findById(carId, username, Action.WRITE);
		if (car == null)
		{
			throw new WrongUserException();
		}
		return car;
	}
}
