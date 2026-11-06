package de.codeflowwizardry.carledger.tracker;

import java.time.Duration;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Snapping the tracks to the road network with a Valhalla service, optional:
 * without a URL the map uses the shared line of the drives
 */
@ConfigMapping(prefix = "carledger.map.matching")
public interface MapMatchingConfig
{
	/**
	 * @return base URL of a self-hosted Valhalla, e.g. http://map-matching:8002;
	 *         a public one would get the positions of the users
	 */
	Optional<String> url();

	/**
	 * @return a matched position further from the GPS position is not used, e.g.
	 *         a parking lot next to a road; meters
	 */
	@WithDefault("50")
	double maxDistance();

	/** @return of a request to the service */
	@WithDefault("60s")
	Duration timeout();

	/** @return the most points of one request, a longer drive is split */
	@WithDefault("2000")
	int maxPoints();

	/**
	 * @return how often the finished drives are matched, e.g. 2m; off: never
	 *         (read by the schedule of {@link MapMatcher})
	 */
	@WithDefault("2m")
	String every();
}
