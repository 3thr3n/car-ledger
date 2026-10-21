package de.codeflowwizardry.carledger.tracker;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.car.track.Track;
import de.codeflowwizardry.carledger.rest.car.track.TrackPoint;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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
	/** tolerance of the simplification per day of the range, meters */
	static final double TOLERANCE_PER_DAY = 5;
	static final double MAX_TOLERANCE = 50;

	private final CarRepository carRepository;
	private final TrackPointRepository trackPointRepository;

	@Inject
	public TrackService(CarRepository carRepository, TrackPointRepository trackPointRepository)
	{
		this.carRepository = carRepository;
		this.trackPointRepository = trackPointRepository;
	}

	/**
	 * Reads the points segment by segment: only one segment of raw points is in
	 * memory at a time.
	 *
	 * @param to
	 *            exclusive
	 */
	@Transactional
	public Track track(long carId, OffsetDateTime from, OffsetDateTime to, String username)
	{
		Utils.requireValid(validate(from, to));
		if (carRepository.findById(carId, username, Action.READ) == null)
		{
			throw new WrongUserException();
		}

		double tolerance = tolerance(Duration.between(from, to));
		Collector collector = new Collector(tolerance);
		trackPointRepository.forEach(carId, from, to, collector::add);
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
	 * @return meters, more for a longer range: the map shows it smaller
	 */
	static double tolerance(Duration range)
	{
		double days = range.toMinutes() / (24.0 * 60);
		return Math.clamp(days * TOLERANCE_PER_DAY, TOLERANCE_PER_DAY, MAX_TOLERANCE);
	}

	/**
	 * Splits the points at the gaps, sums their distance and simplifies each
	 * finished segment
	 */
	private static class Collector
	{
		private final double tolerance;
		private final List<List<TrackPoint>> segments = new ArrayList<>();
		private List<TrackPoint> current = new ArrayList<>();
		private double meters;
		private int count;

		Collector(double tolerance)
		{
			this.tolerance = tolerance;
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
				segments.add(TrackSimplifier.simplify(current, tolerance, SPEED_THRESHOLD));
				current = new ArrayList<>();
			}
		}
	}
}
