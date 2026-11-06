package de.codeflowwizardry.carledger.tracker;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository.Match;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository.Pending;
import de.codeflowwizardry.carledger.tracker.ValhallaClient.NoRoadException;
import de.codeflowwizardry.carledger.tracker.ValhallaClient.Snapped;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Snaps the points of the finished drives to the roads, in the background: a
 * drive is finished when the car sent no point for {@link TrackService#GAP}.
 * The position on the road is stored next to the one of the GPS, which stays;
 * a point without a road near it keeps the one of the GPS (and the shared line
 * on the map). Without the service the points wait and are matched once it is
 * back.
 */
@ApplicationScoped
public class MapMatcher
{
	private static final Logger LOG = LoggerFactory.getLogger(MapMatcher.class);
	/** points of a car read at once */
	static final int BATCH = 20_000;

	private final TrackPointRepository repository;
	private final ValhallaClient client;
	private final MapMatchingConfig config;
	private final MeterRegistry registry;

	@Inject
	public MapMatcher(TrackPointRepository repository, ValhallaClient client, MapMatchingConfig config,
			MeterRegistry registry)
	{
		this.repository = repository;
		this.client = client;
		this.config = config;
		this.registry = registry;
	}

	/** the points of a run */
	public record Summary(int matched, int unmatched)
	{
	}

	@Scheduled(every = "${carledger.map.matching.every:2m}", delayed = "1m", concurrentExecution = ConcurrentExecution.SKIP)
	void scheduled()
	{
		if (config.url().isPresent())
		{
			matchAll();
		}
	}

	/**
	 * Matches the finished drives of all cars, stops when the service isn't
	 * reachable: the points wait for the next run
	 */
	public Summary matchAll()
	{
		OffsetDateTime settled = OffsetDateTime.now().minus(TrackService.GAP);
		int matched = 0;
		int unmatched = 0;
		for (long carId : repository.carsWithPending(settled))
		{
			try
			{
				Summary car = matchCar(carId, settled);
				matched += car.matched;
				unmatched += car.unmatched;
			}
			catch (IOException e)
			{
				LOG.warn("Map matching paused, the service isn't available: {}", e.toString());
				break;
			}
		}
		if (matched + unmatched > 0)
		{
			LOG.info("Matched {} track points to the roads, {} without a road near them", matched, unmatched);
		}
		return new Summary(matched, unmatched);
	}

	private Summary matchCar(long carId, OffsetDateTime settled) throws IOException
	{
		int matched = 0;
		int unmatched = 0;
		while (true)
		{
			List<Pending> pending = repository.pending(carId, settled, BATCH);
			List<List<Pending>> drives = split(pending);
			boolean all = pending.size() < BATCH;
			if (!drives.isEmpty())
			{
				Pending last = drives.getLast().getLast();
				// still on the road, or cut by the batch: with the next run
				if (all ? repository.hasPoint(carId, last.ts(), last.ts().plus(TrackService.GAP))
						: drives.size() > 1)
				{
					drives.removeLast();
				}
			}
			if (drives.isEmpty())
			{
				return new Summary(matched, unmatched);
			}
			for (List<Pending> drive : drives)
			{
				for (int from = 0; from < drive.size(); from += config.maxPoints())
				{
					List<Match> matches = match(drive.subList(from, Math.min(drive.size(), from + config.maxPoints())));
					repository.saveMatches(matches);
					int onRoad = (int) matches.stream()
							.filter(m -> m.state().equals(TrackPointRepository.MATCHED)).count();
					matched += onRoad;
					unmatched += matches.size() - onRoad;
					// carledger_map_matching_points_total, see docs/GPS_TRACKER.md#map-matching
					registry.counter("carledger.map.matching.points", "result", "matched").increment(onRoad);
					registry.counter("carledger.map.matching.points", "result", "unmatched")
							.increment(matches.size() - onRoad);
				}
			}
			if (all)
			{
				return new Summary(matched, unmatched);
			}
		}
	}

	/**
	 * @return the points on the road where a matched position is plausible, the
	 *         others without a road
	 */
	private List<Match> match(List<Pending> points) throws IOException
	{
		List<Snapped> snapped = null;
		if (points.size() > 1)
		{
			try
			{
				snapped = client.match(points);
			}
			catch (NoRoadException e)
			{
				// visible: a whole region outside the map data would look like off-road
				LOG.info("No road for {} points from {}: {}", points.size(), points.getFirst().ts(),
						e.getMessage());
			}
		}
		List<Match> matches = new ArrayList<>(points.size());
		for (int i = 0; i < points.size(); i++)
		{
			Snapped point = snapped == null ? null : snapped.get(i);
			matches.add(point != null && point.distance() <= config.maxDistance()
					? new Match(points.get(i).id(), TrackPointRepository.MATCHED, point.lat(), point.lon())
					: new Match(points.get(i).id(), TrackPointRepository.UNMATCHED, null, null));
		}
		return matches;
	}

	/** the points split into drives at the gaps */
	static List<List<Pending>> split(List<Pending> points)
	{
		List<List<Pending>> drives = new ArrayList<>();
		List<Pending> current = new ArrayList<>();
		for (Pending point : points)
		{
			if (!current.isEmpty() && Duration.between(current.getLast().ts(), point.ts())
					.compareTo(TrackService.GAP) > 0)
			{
				drives.add(current);
				current = new ArrayList<>();
			}
			current.add(point);
		}
		if (!current.isEmpty())
		{
			drives.add(current);
		}
		return drives;
	}
}
