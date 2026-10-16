package de.codeflowwizardry.carledger.notification;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Notifications by e-mail. The SMTP server is configured with quarkus.mailer.*
 * (host, port, from, ...); without enabled nothing is sent.
 */
@ConfigMapping(prefix = "carledger.mail")
public interface MailConfig
{
	@WithDefault("false")
	boolean enabled();

	/**
	 * @return where the app is reachable, e.g. https://car-ledger.example.com,
	 *         for the links in the mails; without one the mails have no link
	 */
	Optional<URI> appUrl();

	/**
	 * @return attempts after the first one failed
	 */
	@WithDefault("3")
	int retries();

	/**
	 * @return wait before the first retry, the further ones wait longer
	 */
	@WithDefault("PT10S")
	Duration backoff();
}
