package de.codeflowwizardry.carledger.map;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * The CSP of the pages of the app, not the API (the attachments send their own,
 * stricter policy): only scripts, fonts and images of this origin (blob: for
 * the
 * previews of uploads), the styles of MUI inline, never in a frame of another
 * site, the service worker of this origin. The map loads its style, tiles,
 * glyphs and sprites from the origins of {@link MapConfig} as well.
 */
@ApplicationScoped
public class ContentSecurityPolicy
{
	private static final Logger LOG = LoggerFactory.getLogger(ContentSecurityPolicy.class);

	static final String HEADER = "Content-Security-Policy";

	/** scheme, host (a leading *. for subdomains) and port, nothing else */
	private static final Pattern ORIGIN = Pattern.compile("https?://(\\*\\.)?[A-Za-z0-9.-]+(:\\d+)?");

	private final String policy;

	@Inject
	public ContentSecurityPolicy(MapConfig config)
	{
		this.policy = policy(mapOrigins(config));
	}

	void install(@Observes Router router)
	{
		// before the static files and Quinoa, like AppCacheHeaders
		router.route().order(-1_000).handler(this::handle);
	}

	void handle(RoutingContext context)
	{
		if (isAppPage(context.normalizedPath()))
		{
			context.addHeadersEndHandler(v -> context.response().headers().set(HEADER, policy));
		}
		context.next();
	}

	static boolean isAppPage(String path)
	{
		return !path.startsWith("/api/") && !path.startsWith("/q/");
	}

	/**
	 * @return the origin of the style and the further sources; an invalid one is
	 *         left out with a warning, it can't extend the policy by other
	 *         directives
	 */
	static Set<String> mapOrigins(MapConfig config)
	{
		Set<String> origins = new LinkedHashSet<>();
		config.styleUrl().ifPresent(url -> {
			try
			{
				URI uri = URI.create(url);
				add(origins, uri.getScheme() + "://" + uri.getRawAuthority());
			}
			catch (IllegalArgumentException e)
			{
				LOG.warn("carledger.map.style-url is no URL: {}", url);
			}
		});
		config.sources().ifPresent(sources -> sources.forEach(source -> add(origins, source.strip())));
		return origins;
	}

	private static void add(Set<String> origins, String origin)
	{
		String withoutSlash = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
		if (ORIGIN.matcher(withoutSlash).matches())
		{
			origins.add(withoutSlash);
		} else
		{
			LOG.warn("Not allowed in the CSP, no origin like https://tiles.example.com: {}", origin);
		}
	}

	static String policy(Set<String> mapOrigins)
	{
		String map = mapOrigins.isEmpty() ? "" : " " + String.join(" ", mapOrigins);
		return "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
				+ "img-src 'self' data: blob:" + map + "; font-src 'self'; connect-src 'self'" + map + "; "
				+ "worker-src 'self'; manifest-src 'self'; object-src 'none'; base-uri 'self'; "
				+ "form-action 'self'; frame-ancestors 'none'";
	}
}
