package de.codeflowwizardry.carledger;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Cache-Control "no-cache" for the frontend files that keep their name in every
 * version (index.html, sw.js, manifest.json, the icons): the browser asks each
 * time and gets a 304 while nothing changed. The hashed files in /assets/ stay
 * cached.
 * <p>
 * Not with quarkus.http.filter: the static files overwrite its Cache-Control.
 */
@ApplicationScoped
public class AppCacheHeaders
{
	static final String NO_CACHE = "no-cache";

	/** before the static files and Quinoa */
	static final int ORDER = -1_000;

	void install(@Observes Router router)
	{
		router.route().order(ORDER).handler(AppCacheHeaders::handle);
	}

	static void handle(RoutingContext context)
	{
		if (isAppShell(context.normalizedPath()))
		{
			context.addHeadersEndHandler(v -> context.response().headers().set(HttpHeaders.CACHE_CONTROL, NO_CACHE));
		}
		context.next();
	}

	/**
	 * @return a file of the frontend without a hash in its name, not the API
	 *         and not the health checks
	 */
	static boolean isAppShell(String path)
	{
		return !path.startsWith("/assets/") && !path.startsWith("/api/") && !path.startsWith("/q/");
	}
}
