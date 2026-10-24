package de.codeflowwizardry.carledger.tracker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Requests per tracker and minute, in memory of this instance. A minute starts
 * with the first request in it (fixed window), old minutes are forgotten on the
 * way.
 */
@ApplicationScoped
public class DeviceRateLimiter
{
	static final long WINDOW_MILLIS = 60_000;

	private final TrackerConfig config;
	private final Map<Long, Window> windows = new ConcurrentHashMap<>();

	private record Window(long start, int count)
	{
	}

	@Inject
	public DeviceRateLimiter(TrackerConfig config)
	{
		this.config = config;
	}

	/**
	 * Counts the request of the tracker
	 *
	 * @return whether it is within the limit
	 */
	public boolean tryAcquire(long deviceId)
	{
		return tryAcquire(deviceId, System.currentTimeMillis());
	}

	boolean tryAcquire(long deviceId, long now)
	{
		if (windows.size() > 1000)
		{
			windows.values().removeIf(window -> now - window.start() >= WINDOW_MILLIS);
		}
		Window window = windows.compute(deviceId, (id, current) -> current == null
				|| now - current.start() >= WINDOW_MILLIS ? new Window(now, 1)
						: new Window(current.start(), current.count() + 1));
		return window.count() <= config.rateLimit();
	}

	/**
	 * @return seconds until the tracker may send again, at least 1
	 */
	public long retryAfterSeconds(long deviceId)
	{
		Window window = windows.get(deviceId);
		if (window == null)
		{
			return 1;
		}
		long left = WINDOW_MILLIS - (System.currentTimeMillis() - window.start());
		return Math.max(1, (left + 999) / 1000);
	}
}
