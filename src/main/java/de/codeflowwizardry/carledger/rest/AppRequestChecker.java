package de.codeflowwizardry.carledger.rest;

import io.quarkus.oidc.JavaScriptRequestChecker;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Which requests without a session get the 499 of quarkus-oidc instead of
 * the redirect to the login: only the ones of the frontend, which sends
 * {@value RequestedWith#HEADER}: <code>JavaScript</code> and opens the login
 * itself (see QueryClient.ts).
 * <p>
 * By default quarkus-oidc also answers <code>XMLHttpRequest</code> with the
 * 499. Some browsers built into apps (e.g. of WhatsApp) send it with the
 * opening of a page: the click on "Login" then got an empty 499 without a
 * content type, which Safari saved as a file "login" of 0 KB. A navigation of
 * the browser (<code>Sec-Fetch-Mode: navigate</code>) always gets the
 * redirect.
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
