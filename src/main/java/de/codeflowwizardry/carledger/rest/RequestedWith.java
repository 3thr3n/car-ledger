package de.codeflowwizardry.carledger.rest;

import java.util.Set;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * CSRF protection next to <code>SameSite=Strict</code>, which doesn't separate
 * the hosts of one site (e.g. keycloak.&lt;host&gt;): a change with the login
 * session needs the header {@value #HEADER}, which another origin can't set
 * without a CORS preflight. Requests with a token (<code>Authorization</code>)
 * don't need it.
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
