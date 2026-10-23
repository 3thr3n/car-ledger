package de.codeflowwizardry.carledger.tracker;

import java.time.Duration;
import java.util.Optional;

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

	/**
	 * @return directory with a release of the tracker firmware (manifest.json and
	 *         the files it lists), logged-in users flash it from the browser.
	 *         Without one, flashing is hidden.
	 */
	Optional<String> firmwareDir();

	/**
	 * @return a drive of the GPS tracks ends after a stop this long: no points, or
	 *         a car that doesn't move. Drives are suggested as trips.
	 */
	@WithDefault("5M")
	Duration stop();

	/**
	 * @return meters of GPS distance a drive needs to be suggested as a trip
	 */
	@WithDefault("500")
	int minDriveDistance();

	/**
	 * @return how long a drive needs to be suggested as a trip
	 */
	@WithDefault("2M")
	Duration minDriveDuration();
}
