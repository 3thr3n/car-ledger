package de.codeflowwizardry.carledger.notification.push;

import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@ApplicationScoped
public class VapidKeyProducer
{
	private static final Logger LOG = LoggerFactory.getLogger(VapidKeyProducer.class);

	/**
	 * @return the key of carledger.push.private-key, else a new one
	 * @throws IllegalStateException
	 *             if the configured key cannot be read
	 */
	@Produces
	@Singleton
	VapidKeys vapidKeys(PushConfig config)
	{
		if (config.privateKey().filter(key -> !key.isBlank()).isEmpty())
		{
			if (config.enabled())
			{
				LOG.warn("No carledger.push.private-key, the push subscriptions end with the next restart");
			}
			return VapidKeys.generate();
		}
		try
		{
			return VapidKeys.fromPem(config.privateKey().get());
		}
		catch (GeneralSecurityException | IllegalArgumentException e)
		{
			throw new IllegalStateException("carledger.push.private-key is no P-256 key (EC PRIVATE KEY)", e);
		}
	}
}
