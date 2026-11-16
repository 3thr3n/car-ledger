package de.codeflowwizardry.carledger.rest;

import io.quarkus.oidc.JavaScriptRequestChecker;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Requests without a session get the 499 of quarkus-oidc instead of the
 * redirect
 * to the login only when they come from the frontend
 * ({@value RequestedWith#HEADER}: <code>JavaScript</code>), which opens the
 * login
 * itself. Not for <code>XMLHttpRequest</code>, which in-app browsers send with
 * a
 * page navigation, and never for <code>Sec-Fetch-Mode: navigate</code>.
 */
@ApplicationScoped
public class AppRequestChecker implements JavaScriptRequestChecker
{
	static final String APP = "JavaScript";

	static final String FETCH_MODE = "Sec-Fetch-Mode";

	@Override
	public boolean isJavaScriptRequest(RoutingContext context)
	{
		return isApp(context.request().getHeader(RequestedWith.HEADER), context.request().getHeader(FETCH_MODE));
	}

	/**
	 * @return whether the request is one of the frontend, which handles the
	 *         missing session itself
	 */
	static boolean isApp(String requestedWith, String fetchMode)
	{
		return APP.equals(requestedWith) && !"navigate".equalsIgnoreCase(fetchMode);
	}
}
