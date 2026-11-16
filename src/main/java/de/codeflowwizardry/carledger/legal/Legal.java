package de.codeflowwizardry.carledger.legal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The links to the legal pages, checked at the start: they end up in the
 * <code>href</code> of the app and in the mails, so only absolute http(s)
 * URLs, never e.g. <code>javascript:</code>
 */
@Startup
@ApplicationScoped
public class Legal
{
	private final String imprintUrl;
	private final String privacyUrl;

	@Inject
	public Legal(LegalConfig config)
	{
		this.imprintUrl = checked("carledger.legal.imprint-url", config.imprintUrl());
		this.privacyUrl = checked("carledger.legal.privacy-url", config.privacyUrl());
	}

	/** @return the Impressum, <code>null</code> without one */
	public String imprintUrl()
	{
		return imprintUrl;
	}

	/** @return the privacy policy, <code>null</code> without one */
	public String privacyUrl()
	{
		return privacyUrl;
	}

	/**
	 * @return the URL, <code>null</code> for none or a blank one
	 * @throws IllegalStateException
	 *             if it isn't an absolute http(s) URL: the app doesn't start
	 */
	static String checked(String property, Optional<String> value)
	{
		if (value.isEmpty() || value.get().isBlank())
		{
			return null;
		}
		String url = value.get().strip();
		try
		{
			URI uri = new URI(url);
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			if ((scheme.equals("https") || scheme.equals("http")) && uri.getHost() != null)
			{
				return url;
			}
		}
		catch (URISyntaxException e)
		{
			// below
		}
		throw new IllegalStateException(
				property + " must be an absolute http(s) URL, e.g. https://example.com/imprint: "
						+ url);
	}
}
