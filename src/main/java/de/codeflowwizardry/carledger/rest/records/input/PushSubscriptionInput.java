package de.codeflowwizardry.carledger.rest.records.input;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * A subscription of the browser (PushSubscription.toJSON()), the keys
 * base64url
 *
 * @param endpoint
 *            URL of the push service for the browser
 * @param p256dh
 *            public key of the browser
 * @param auth
 *            secret of the browser
 */
public record PushSubscriptionInput(String endpoint, String p256dh, String auth)
{
	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (endpoint == null || endpoint.length() > 2048 || !isHttps(endpoint))
		{
			errors.add("The endpoint must be an https URL!");
		}
		if (length(p256dh) != 65)
		{
			errors.add("p256dh must be a P-256 key!");
		}
		if (length(auth) != 16)
		{
			errors.add("auth must have 16 bytes!");
		}
		return errors;
	}

	private static boolean isHttps(String url)
	{
		try
		{
			URI uri = URI.create(url);
			return "https".equals(uri.getScheme()) && uri.getHost() != null;
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
	}

	private static int length(String base64url)
	{
		try
		{
			return base64url == null ? -1 : Base64.getUrlDecoder().decode(base64url).length;
		}
		catch (IllegalArgumentException e)
		{
			return -1;
		}
	}
}
