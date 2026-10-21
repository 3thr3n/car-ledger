package de.codeflowwizardry.carledger.tracker;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * GPS trackers that send their points to <code>/api/device/**</code>
 */
@ConfigMapping(prefix = "carledger.tracker")
public interface TrackerConfig
{
	/**
	 * @return requests of a tracker per minute, more are answered with 429. Counted
	 *         per instance of CarLedger.
	 */
	@WithDefault("60")
	int rateLimit();

	/**
	 * @return points in one request, a larger batch is answered with 400
	 */
	@WithDefault("500")
	int maxBatch();
}
