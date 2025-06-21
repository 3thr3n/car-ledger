package de.codeflowwizardry.carledger.state;

import de.codeflowwizardry.carledger.client.KeycloakTokenResponse;
import de.codeflowwizardry.carledger.client.KeycloakTokenService;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashSet;

@ApplicationScoped
@Priority(100) // Should run early
public class BffAuthenticationMechanism implements HttpAuthenticationMechanism {

    private final static Logger LOG = LoggerFactory.getLogger(BffAuthenticationMechanism.class);

    @Inject
    SessionManager sessionManager;

    @Inject
    JWTParser jwtParser;

    @Inject
    @RestClient
    KeycloakTokenService keycloakTokenService;

    @ConfigProperty(name = "keycloak.client-id")
    String clientId;

    @ConfigProperty(name = "keycloak.client-secret")
    String clientSecret;

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager idpManager) {
        Cookie cookie = context.request().getCookie("SESSION_ID");

        if (cookie != null) {
            try {
                SecurityIdentity identity = handleCookie(cookie);
                return Uni.createFrom().item(identity);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            } catch (AuthenticationFailedException e) {
                return Uni.createFrom().failure(e);
            }
        }
        LOG.debug("No cookie found, be anonymous");
        return Uni.createFrom().item(QuarkusSecurityIdentity.builder().setPrincipal(() -> "anonymous").setAnonymous(true).build());
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        // No challenge needed since auth happens earlier
        return Uni.createFrom().nothing();
    }

    private SecurityIdentity handleCookie(Cookie cookie) throws ParseException {
        String sessionId = cookie.getValue();
        SessionToken token = sessionManager.get(sessionId);

        if (token != null) {
            try {
                JsonWebToken jwt = jwtParser.parse(token.getAccessToken());
                return handleJwtToken(jwt, token, sessionId);
            } catch (ParseException e) {
                if (e.getCause() instanceof InvalidJwtException && e.getCause().getMessage().contains("Expiration Time")) {
                    LOG.debug("Token expired");
                    refreshToken(token, sessionId);
                    JsonWebToken jwt = jwtParser.parse(token.getAccessToken());
                    return handleJwtToken(jwt, token, sessionId);
                } else {
                    throw e;
                }
            }
        }
        LOG.debug("Session token invalid");
        throw new AuthenticationFailedException("Invalid session token");
    }

    private QuarkusSecurityIdentity handleJwtToken(JsonWebToken jwt, SessionToken token, String sessionId) throws ParseException {
        long exp = jwt.getExpirationTime();
        if (exp > 0 && exp < Instant.now().getEpochSecond()) {
            refreshToken(token, sessionId);
            jwt = jwtParser.parse(token.getAccessToken());
        }

        Object preferredUsername = jwt.getClaim("preferred_username");
        return QuarkusSecurityIdentity.builder()
                .setPrincipal(preferredUsername::toString)
                .addAttribute("name", jwt.getClaim("name"))
                .addAttribute("email", jwt.getClaim("email"))
                .addRoles(new HashSet<>(jwt.getGroups()))
                .build();
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
}
