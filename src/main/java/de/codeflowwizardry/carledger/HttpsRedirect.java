package de.codeflowwizardry.carledger;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Redirects a browser that opened the app with http to https. The gateway
 * can't,
 * as cert-manager answers its HTTP-01 challenge with http on the same host.
 * <p>
 * Only when the gateway reports http ({@value #FORWARDED_PROTO}), so the
 * probes,
 * dev and the tests keep plain http; only GET and HEAD, a redirected change
 * would
 * lose its body.
 */
@ApplicationScoped
public class HttpsRedirect
{
	static final String FORWARDED_PROTO = "X-Forwarded-Proto";

	/** before everything else, also the login and the cache headers */
	static final int ORDER = -2_000;

	void install(@Observes Router router)
	{
		router.route().order(ORDER).handler(HttpsRedirect::handle);
	}

	static void handle(RoutingContext context)
	{
		String host = context.request().getHeader(HttpHeaders.HOST);
		if (isPlainHttp(context.request().method(), context.request().getHeader(FORWARDED_PROTO), host))
		{
			context.response().setStatusCode(301)
					.putHeader(HttpHeaders.LOCATION, "https://" + host + context.request().uri())
					.end();
			return;
		}
		context.next();
	}

	/**
	 * @return whether the browser read a page or file through the gateway with
	 *         http
	 */
	static boolean isPlainHttp(HttpMethod method, String forwardedProto, String host)
	{
		return (HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method))
				&& "http".equalsIgnoreCase(forwardedProto) && host != null && !host.isBlank();
	}
}
