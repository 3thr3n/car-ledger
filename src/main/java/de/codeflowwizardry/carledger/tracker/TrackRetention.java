package de.codeflowwizardry.carledger.tracker;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.car.track.TrackRetentionInput;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * How long the GPS track points of a car are kept: they are personal data, the
 * owner sets the days or deletes them all. Older points are deleted every
 * night.
 */
@ApplicationScoped
public class TrackRetention
{
	private static final Logger LOG = LoggerFactory.getLogger(TrackRetention.class);

	private final CarRepository carRepository;
	private final TrackPointRepository trackPointRepository;

	@Inject
	public TrackRetention(CarRepository carRepository, TrackPointRepository trackPointRepository)
	{
		this.carRepository = carRepository;
		this.trackPointRepository = trackPointRepository;
	}

	/**
	 * The older points go with the next cleanup
	 */
	@Transactional
	public void setRetention(long carId, TrackRetentionInput input, String username)
	{
		Utils.requireValid(input.validate());
		owned(carId, username).setTrackRetentionDays(input.days());
	}

	/**
	 * @return the amount of points deleted
	 */
	@Transactional
	public int deleteAll(long carId, String username)
	{
		return trackPointRepository.delete(owned(carId, username).getId(), null);
	}

	private CarEntity owned(long carId, String username)
	{
		CarEntity car = carRepository.findById(carId, username, Action.MANAGE);
		if (car == null)
		{
			throw new WrongUserException();
		}
		return car;
	}

	/**
	 * Deletes the points older than the retention of their car. Every instance
	 * runs it, deleting twice does no harm.
	 */
	@Scheduled(cron = "0 30 2 * * ?")
	void cleanup()
	{
		cleanup(OffsetDateTime.now());
	}

	/**
	 * @return the amount of points deleted
	 */
	int cleanup(OffsetDateTime now)
	{
		List<CarEntity> cars = QuarkusTransaction.requiringNew()
				.call(() -> carRepository.list("trackRetentionDays is not null"));

		int deleted = 0;
		for (CarEntity car : cars)
		{
			try
			{
				// one transaction per car, a failing car must not keep the points of the others
				deleted += QuarkusTransaction.requiringNew().call(() -> trackPointRepository.delete(car.getId(),
						now.minusDays(car.getTrackRetentionDays())));
			}
			catch (Exception e)
			{
				LOG.error("Could not delete the old track points of car {}", car.getId(), e);
			}
		}
		if (deleted > 0)
		{
			LOG.info("Deleted {} track points older than the retention of their car", deleted);
		}
		return deleted;
	}
}
