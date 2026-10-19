package de.codeflowwizardry.carledger;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * The browser asks again for the files of the frontend that keep their name in
 * every version: index.html (also of the pages of the app), sw.js,
 * manifest.json and the icons. The static files are served with
 * "public, immutable, max-age=86400", so a browser kept an old version for a
 * day, even on a reload. With "no-cache" it asks every time and gets a 304
 * while nothing changed. The files of the build in /assets/ have a hash in
 * their name and stay cached.
 * <p>
 * Not with quarkus.http.filter: its headers are set before the static files,
 * which overwrite Cache-Control.
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
