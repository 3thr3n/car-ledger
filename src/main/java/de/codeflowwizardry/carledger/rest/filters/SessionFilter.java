package de.codeflowwizardry.carledger.rest.filters;

import de.codeflowwizardry.carledger.client.KeycloakTokenResponse;
import de.codeflowwizardry.carledger.client.KeycloakTokenService;
import de.codeflowwizardry.carledger.state.SessionContext;
import de.codeflowwizardry.carledger.state.SessionManager;
import de.codeflowwizardry.carledger.state.SessionToken;
import de.codeflowwizardry.carledger.state.SessionUser;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
public class SessionFilter implements ContainerRequestFilter {

    private final static Logger LOG = LoggerFactory.getLogger(SessionFilter.class);

    private final SessionManager sessionManager;
    private final SessionContext sessionContext;
    private final JWTParser jwtParser;
    private final KeycloakTokenService keycloakTokenService;

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login",
            "/auth/logout",
            "/public"
    );

    @ConfigProperty(name = "keycloak.client-id")
    String clientId;

    @ConfigProperty(name = "keycloak.client-secret")
    String clientSecret;

    @Inject
    public SessionFilter(SessionManager sessionManager, SessionContext sessionContext, JWTParser jwtParser, @RestClient KeycloakTokenService keycloakTokenService) {
        this.sessionManager = sessionManager;
        this.sessionContext = sessionContext;
        this.jwtParser = jwtParser;
        this.keycloakTokenService = keycloakTokenService;
    }

    @Override
    public void filter(ContainerRequestContext ctx) {
        LOG.debug("Filtering request");
        String path = ctx.getUriInfo().getPath();

        if (isPublicPath(path)) {
            LOG.debug("Skip path: {}", path);
            return; // Skip auth check for public endpoints
        }
        LOG.debug("Not skipping path: {}", path);

        Cookie cookie = ctx.getCookies().get("SESSION_ID");
        if (cookie == null) {
            ctx.abortWith(Response.status(UNAUTHORIZED).build());
            return;
        }

        try {
            handleCookie(cookie);
        }
        catch (ParseException e) {
            LOG.error("Error handling JWT", e);
            ctx.abortWith(Response.status(UNAUTHORIZED).build());
        }
        catch (Exception e) {
            LOG.error("Error handling cookies", e);
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private void handleCookie(Cookie cookie) throws ParseException {
        String sessionId = cookie.getValue();
        SessionToken token = sessionManager.get(sessionId);

        if (token != null) {
            try {
                JsonWebToken jwt = jwtParser.parse(token.getAccessToken());
                handleJwtToken(jwt, token, sessionId);
            } catch (ParseException e) {
                if (e.getCause() instanceof InvalidJwtException && e.getCause().getMessage().contains("Expiration Time")) {
                    LOG.debug("Token expired");
                    refreshToken(token, sessionId);
                    JsonWebToken jwt = jwtParser.parse(token.getAccessToken());
                    handleJwtToken(jwt, token, sessionId);
                    return;
                }
                throw e;
            }
        }
    }

    private void handleJwtToken(JsonWebToken jwt, SessionToken token, String sessionId) throws ParseException {
        long exp = jwt.getExpirationTime();
        if (exp > 0 && exp < Instant.now().getEpochSecond()) {
            refreshToken(token, sessionId);
            jwt = jwtParser.parse(token.getAccessToken());
        }

        SessionUser user = new SessionUser(
                jwt.getClaim("preferred_username"),
                jwt.getClaim("name"),
                jwt.getClaim("email"),
                jwt.getGroups() != null ? List.copyOf(jwt.getGroups()) : List.of()
        );
        sessionContext.set(user);
    }

    private void refreshToken(SessionToken token, String sessionId) {
        // Try to refresh
        KeycloakTokenResponse refreshed = keycloakTokenService.refreshToken(
                "refresh_token",
                clientId,
                clientSecret,
                token.getRefreshToken());


        token.setAccessToken(refreshed.accessToken());
        sessionManager.updateAccessToken(sessionId, token.getAccessToken());
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.contains(path);
    }
}