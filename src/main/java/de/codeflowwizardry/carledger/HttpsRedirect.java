package de.codeflowwizardry.carledger;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Sends a browser that opened the app with http to https. The gateway itself
 * can't force https: the HTTP-01 challenge of cert-manager is answered with
 * http, by its own solver behind the same host. That path never reaches the
 * app, so the app redirects everything else.
 * <p>
 * Only when the gateway reports http ({@value #FORWARDED_PROTO}): the probes
 * of Kubernetes, dev and the tests call the app directly with http, without
 * the header. Only reads (GET, HEAD) are redirected, a change with http (e.g.
 * of a tracker with a wrong URL) would be sent again without its body.
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
