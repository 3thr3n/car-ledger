package de.codeflowwizardry.carledger.tracker;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.DeviceEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.DeviceRepository;
import de.codeflowwizardry.carledger.data.repository.TrackPointRepository;
import de.codeflowwizardry.carledger.exception.WrongUserException;
import de.codeflowwizardry.carledger.rest.car.tracker.Device;
import de.codeflowwizardry.carledger.rest.car.tracker.DeviceCreated;
import de.codeflowwizardry.carledger.rest.car.tracker.DeviceInput;
import de.codeflowwizardry.carledger.rest.device.IngestResult;
import de.codeflowwizardry.carledger.rest.device.TrackPointInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

/**
 * The GPS trackers of the cars: the owner creates and revokes them, a tracker
 * sends its points with its token
 */
@ApplicationScoped
public class DeviceService
{
	/** the last contact of a tracker is written at most this often */
	static final long LAST_SEEN_MINUTES = 1;

	private final DeviceRepository deviceRepository;
	private final CarRepository carRepository;
	private final TrackPointRepository trackPointRepository;

	@Inject
	public DeviceService(DeviceRepository deviceRepository, CarRepository carRepository,
			TrackPointRepository trackPointRepository)
	{
		this.deviceRepository = deviceRepository;
		this.carRepository = carRepository;
		this.trackPointRepository = trackPointRepository;
	}

	/**
	 * A tracker that sent a valid token
	 */
	public record AuthenticatedDevice(long deviceId, long carId)
	{
	}

	public List<Device> list(long carId, String username)
	{
		return deviceRepository.list(carId, username).stream().map(Device::convert).toList();
	}

	@Transactional
	public DeviceCreated create(long carId, DeviceInput input, String username)
	{
		Utils.requireValid(input.validate());

		CarEntity car = carRepository.findById(carId, username, Action.MANAGE);
		if (car == null)
		{
			throw new WrongUserException();
		}

		String token = DeviceTokens.generate();
		DeviceEntity entity = new DeviceEntity(car, input.name().strip(), DeviceTokens.hash(token));
		deviceRepository.persist(entity);
		return new DeviceCreated(Device.convert(entity), token);
	}

	/**
	 * A new token for the tracker, e.g. to set it up again after its token got
	 * lost; the old token stops working, the tracker and its points stay.
	 *
	 * @throws ClientErrorException
	 *             409 for a revoked tracker, it stays revoked
	 */
	@Transactional
	public DeviceCreated renewToken(long carId, long id, String username)
	{
		DeviceEntity entity = deviceRepository.find(carId, id, username)
				.orElseThrow(() -> new NotFoundException("Tracker not found for the car of your user."));
		if (entity.getRevokedAt() != null)
		{
			throw new ClientErrorException(Response.status(Response.Status.CONFLICT)
					.entity("The tracker is revoked!").build());
		}
		String token = DeviceTokens.generate();
		entity.setTokenHash(DeviceTokens.hash(token));
		return new DeviceCreated(Device.convert(entity), token);
	}

	/**
	 * The token stops working, the points of the tracker stay. Revoking again
	 * keeps the first time.
	 */
	@Transactional
	public Device revoke(long carId, long id, String username)
	{
		DeviceEntity entity = deviceRepository.find(carId, id, username)
				.orElseThrow(() -> new NotFoundException("Tracker not found for the car of your user."));
		entity.revoke();
		return Device.convert(entity);
	}

	/**
	 * @return the tracker of the token, if it isn't revoked; its last contact is
	 *         updated
	 */
	@Transactional
	public Optional<AuthenticatedDevice> authenticate(String token)
	{
		return deviceRepository.findActiveByTokenHash(DeviceTokens.hash(token))
				.map(device -> {
					OffsetDateTime now = OffsetDateTime.now();
					deviceRepository.touch(device.getId(), now, now.minusMinutes(LAST_SEEN_MINUTES));
					return new AuthenticatedDevice(device.getId(), device.getCar().getId());
				});
	}

	/**
	 * Stores the valid points for the car of the tracker, invalid ones are skipped
	 * and points it sent before are dropped. Implausible g-forces are left out,
	 * their point stays.
	 */
	@Transactional
	public IngestResult ingest(AuthenticatedDevice device, List<TrackPointInput> points)
	{
		OffsetDateTime now = OffsetDateTime.now();
		List<TrackPointInput> valid = points.stream().filter(point -> point != null && point.isValid(now))
				.map(TrackPointInput::plausible).toList();
		int accepted = valid.isEmpty() ? 0 : trackPointRepository.insert(device.deviceId(), device.carId(), valid);
		return new IngestResult(accepted, valid.size() - accepted, points.size() - valid.size());
	}
}
