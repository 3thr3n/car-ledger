package de.codeflowwizardry.carledger.notification.push;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Notifications as Web Push to the browsers that subscribed, without enabled
 * nothing is sent
 */
@ConfigMapping(prefix = "carledger.push")
public interface PushConfig
{
	@WithDefault("false")
	boolean enabled();

	/**
	 * @return the key of the server as "EC PRIVATE KEY" (PEM); without one a
	 *         key is generated at the start, the subscriptions end with every
	 *         restart
	 */
	Optional<String> privateKey();

	/**
	 * @return contact of the operator for the push services, mailto: or https:
	 */
	@WithDefault("https://github.com/3thr3n/car-ledger")
	String subject();

	/**
	 * @return how long a push service keeps a message for a browser that is
	 *         offline
	 */
	@WithDefault("PT24H")
	Duration ttl();

	/**
	 * @return attempts after the first one failed
	 */
	@WithDefault("3")
	int retries();

	/**
	 * @return wait before the first retry, each further one waits four times as
	 *         long
	 */
	@WithDefault("PT10S")
	Duration backoff();

	@WithDefault("PT10S")
	Duration timeout();

	/**
	 * @return the hosts of the push services the browsers subscribe at, a leading
	 *         "*." for their subdomains: Google (Chrome, Edge on Android), Mozilla
	 *         (Firefox), Apple (Safari), Microsoft (Edge). Messages only go there,
	 *         a subscription cannot point the server to other hosts.
	 */
	@WithDefault("*.googleapis.com,*.google.com,*.push.services.mozilla.com,*.push.apple.com,*.notify.windows.com")
	List<String> allowedHosts();
}
