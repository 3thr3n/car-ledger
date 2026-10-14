package de.codeflowwizardry.carledger.webhook;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Where and how the events are sent, without a URL nothing is sent
 */
@ConfigMapping(prefix = "carledger.webhook")
public interface WebhookConfig
{
	/**
	 * @return receiver of the events
	 */
	Optional<URI> url();

	/**
	 * @return key of the HMAC-SHA256 signature, without one the events are not
	 *         signed
	 */
	Optional<String> secret();

	/**
	 * @return the types to send, e.g. car-request.created; all without a list
	 */
	Optional<List<String>> events();

	/**
	 * @return attempts after the first one failed
	 */
	@WithDefault("3")
	int retries();

	/**
	 * @return wait before the first retry, each further one waits four times as
	 *         long
	 */
	@WithDefault("PT2S")
	Duration backoff();

	@WithDefault("PT10S")
	Duration timeout();
}
