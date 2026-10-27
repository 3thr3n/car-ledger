package de.codeflowwizardry.carledger.notification.push;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Which endpoints a subscription may have: https on the default port at a
 * known push service; http only on a port named explicitly (the tests). Without
 * the check, a user could make the server send
 * requests to any host, also inside the network of the server.
 */
public final class PushEndpoints
{
	private static final Pattern IP_ADDRESS = Pattern.compile("^[0-9.]+$|^\\[?[0-9a-f:]+]?$");

	private PushEndpoints()
	{
	}

	/**
	 * @param allowedHosts
	 *            e.g. fcm.googleapis.com or *.googleapis.com for the host and its
	 *            subdomains, on the default port; with ":port" or ":*" on that
	 *            or any port (the tests)
	 */
	public static boolean isAllowed(String endpoint, List<String> allowedHosts)
	{
		URI uri;
		try
		{
			uri = URI.create(endpoint);
		}
		catch (IllegalArgumentException | NullPointerException e)
		{
			return false;
		}
		String host = uri.getHost();
		boolean https = "https".equals(uri.getScheme());
		if (host == null || uri.getUserInfo() != null || (!https && !"http".equals(uri.getScheme())))
		{
			return false;
		}
		int port = uri.getPort() == -1 ? defaultPort(uri) : uri.getPort();
		host = host.toLowerCase(Locale.ROOT);
		if (IP_ADDRESS.matcher(host).matches())
		{
			return false;
		}
		for (String allowed : allowedHosts)
		{
			String pattern = allowed.trim().toLowerCase(Locale.ROOT);
			String allowedPort = String.valueOf(defaultPort(uri));
			int colon = pattern.indexOf(':');
			if (colon >= 0)
			{
				allowedPort = pattern.substring(colon + 1);
				pattern = pattern.substring(0, colon);
			} else if (!https)
			{
				continue;
			}
			boolean hostMatches = pattern.startsWith("*.")
					? host.endsWith(pattern.substring(1)) && host.length() > pattern.length() - 1
					: host.equals(pattern);
			if (hostMatches && ("*".equals(allowedPort) || allowedPort.equals(String.valueOf(port))))
			{
				return true;
			}
		}
		return false;
	}

	private static int defaultPort(URI uri)
	{
		return "http".equals(uri.getScheme()) ? 80 : 443;
	}
}
