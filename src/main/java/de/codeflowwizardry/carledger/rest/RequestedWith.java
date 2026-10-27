package de.codeflowwizardry.carledger.rest;

import java.util.Set;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Against CSRF besides the cookie with <code>SameSite=Strict</code>: a request
 * of the login session that changes something needs the header
 * {@value #HEADER}, which the frontend sends with every request. SameSite
 * doesn't separate the hosts of one site, e.g. keycloak.&lt;host&gt; next to
 * car-ledger.&lt;host&gt;, and a form there posts multipart without a
 * preflight. A page of another origin can't set the header, it would need a
 * preflight, which fails without CORS.
 * <p>
 * Requests with a token (<code>Authorization</code>, service accounts and GPS
 * trackers) are not affected, also with a cookie: a form of another page
 * can't set the header either.
 */
@ApplicationScoped
public class RequestedWith
{
	static final String HEADER = "X-Requested-With";

	/** the cookie of the session of quarkus-oidc, also its chunks */
	static final String SESSION_COOKIE = "q_session";

	/** before the body is read, e.g. of an upload */
	static final int ORDER = -900;

	private static final Set<HttpMethod> SAFE = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

	void install(@Observes Router router)
	{
		router.route("/api/*").order(ORDER).handler(RequestedWith::handle);
	}

	static void handle(RoutingContext context)
	{
		if (isForged(context.request().method(), context.request().getHeader(HttpHeaders.COOKIE),
				context.request().getHeader(HttpHeaders.AUTHORIZATION), context.request().getHeader(HEADER)))
		{
			context.response().setStatusCode(403).end("Requests of the app need the header " + HEADER);
			return;
		}
		context.next();
	}

	/**
	 * @return whether the request changes something with the cookie of the
	 *         session but without the header
	 */
	static boolean isForged(HttpMethod method, String cookies, String authorization, String requestedWith)
	{
		return !SAFE.contains(method) && cookies != null && cookies.contains(SESSION_COOKIE) && authorization == null
				&& (requestedWith == null || requestedWith.isBlank());
	}
}
