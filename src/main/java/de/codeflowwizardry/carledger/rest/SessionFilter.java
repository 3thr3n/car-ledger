package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.client.KeycloakTokenResponse;
import de.codeflowwizardry.carledger.state.SessionContext;
import de.codeflowwizardry.carledger.state.SessionManager;
import de.codeflowwizardry.carledger.state.SessionUser;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class SessionFilter implements ContainerRequestFilter {

    private final static Logger LOG = LoggerFactory.getLogger(SessionFilter.class);

    private final SessionManager sessionManager;
    private final SessionContext sessionContext;
    private final JWTParser jwtParser;

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login",
            "/auth/logout",
            "/public"
    );

    @Inject
    public SessionFilter(SessionManager sessionManager, SessionContext sessionContext, JWTParser jwtParser) {
        this.sessionManager = sessionManager;
        this.sessionContext = sessionContext;
        this.jwtParser = jwtParser;
    }

    @Override
    public void filter(ContainerRequestContext ctx) {
        LOG.info("Filtering request");
        String path = ctx.getUriInfo().getPath();

        if (isPublicPath(path)) {
            LOG.info("Skip path: {}", path);
            return; // Skip auth check for public endpoints
        }
        LOG.info("Not skipping path: {}", path);

        Cookie cookie = ctx.getCookies().get("SESSION_ID");
        if (cookie != null) {
            try {
                handleCookie(cookie);
            } catch (Exception e) {
                ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            }
        }
        ctx.abortWith(Response.status(UNAUTHORIZED).build());
    }

    private void handleCookie(Cookie cookie) throws ParseException {
        String sessionId = cookie.getValue();
        KeycloakTokenResponse token = sessionManager.get(sessionId);
        if (token != null) {
            LOG.info("Expires in: {}", token.expires_in());

            String accessToken = token.access_token();
            JsonWebToken jwt = jwtParser.parse(accessToken);
            SessionUser user = new SessionUser(
                    jwt.getClaim("preferred_username"),
                    jwt.getClaim("name"),
                    jwt.getClaim("email"),
                    jwt.getGroups() != null ? List.copyOf(jwt.getGroups()) : List.of()
            );
            sessionContext.set(user);
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.contains(path);
    }
}