package de.codeflowwizardry.carledger.state;

import de.codeflowwizardry.carledger.client.KeycloakTokenResponse;
import de.codeflowwizardry.carledger.client.KeycloakTokenService;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
@Priority(100) // Should run early
public class BffAuthenticationMechanism implements HttpAuthenticationMechanism {

    private final static Logger LOG = LoggerFactory.getLogger(BffAuthenticationMechanism.class);

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/public");

    private final SessionManager sessionManager;
    private final JWTParser jwtParser;
    private final KeycloakTokenService keycloakTokenService;

    @Inject
    public BffAuthenticationMechanism(SessionManager sessionManager, JWTParser jwtParser,
                                      @RestClient KeycloakTokenService keycloakTokenService) {
        this.sessionManager = sessionManager;
        this.jwtParser = jwtParser;
        this.keycloakTokenService = keycloakTokenService;
    }

    @ConfigProperty(name = "keycloak.client-id")
    String clientId;

    @ConfigProperty(name = "keycloak.client-secret")
    String clientSecret;

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager idpManager) {
        return Uni.createFrom().deferred(() -> {
            String path = context.request().path();
            LOG.debug("Authenticating request for path {}", path);
            if (isPublicPath(path)) {
                LOG.debug("Skip path: {}", path);
                return Uni.createFrom().item(
                        QuarkusSecurityIdentity.builder().setAnonymous(true).setPrincipal(() -> "anonymous").build());
            }

            Cookie cookie = context.request().getCookie("SESSION_ID");

            if (cookie == null) {
                LOG.debug("No cookie found, be anonymous");
                return Uni.createFrom().item(
                        QuarkusSecurityIdentity.builder().setAnonymous(true).setPrincipal(() -> "anonymous").build());
            }

            LOG.debug("Cookie found: {}", cookie.getValue());
            try {
                return handleCookie(cookie);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                // return Uni.createFrom().optional(Optional.empty());
                return Uni.createFrom().failure(e);
            }
        });
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        // No challenge needed since auth happens earlier
        ChallengeData result = new ChallengeData(HttpResponseStatus.UNAUTHORIZED.code(), HttpHeaderNames.COOKIE,
                "Cookie");
        return Uni.createFrom().item(result);
    }

    private Uni<? extends SecurityIdentity> handleCookie(Cookie cookie) {
        String sessionId = cookie.getValue();
        SessionToken token = sessionManager.get(sessionId);

        if (token == null) {
            LOG.debug("Session token invalid");
            return Uni.createFrom().failure(new AuthenticationFailedException("Invalid session token"));
        }

        LOG.debug("Token found, parsing JWT...");

        return tryParseJwt(token.getAccessToken())
                .onItem()
                .transformToUni(jwt -> {
                    // token is valid
                    return handleJwtToken(jwt, token, sessionId);
                })
                .onFailure(Exception.class)
                .recoverWithUni(err -> {
                    if (err.getCause() instanceof ParseException) {
                    //  err.getCause().getMessage().contains("Expiration Time")) {
                        LOG.debug("JWT expired, attempting refresh");

                        return refreshToken(token, sessionId)
                                .onItem().transformToUni(refreshed -> tryParseJwt(token.getAccessToken())
                                        .onItem().transformToUni(jwt -> handleJwtToken(jwt, token, sessionId)));
//                    } else {
//                        LOG.debug("Cause is different: {}", String.valueOf(err.getCause()));
//                        return Uni.createFrom().failure(err);
                    }
                    return Uni.createFrom().failure(err);
                });
    }

    private Uni<JsonWebToken> tryParseJwt(String token) {
        return Uni.createFrom().item(Unchecked.supplier(() -> jwtParser.parse(token)));
    }

    private Uni<KeycloakTokenResponse> refreshToken(SessionToken token, String sessionId) {
        return keycloakTokenService.refreshToken(
                        "refresh_token",
                        clientId,
                        clientSecret,
                        token.getRefreshToken())
                .onItem().invoke(refreshed -> {
                    token.setAccessToken(refreshed.accessToken());
                    sessionManager.updateAccessToken(sessionId, refreshed.accessToken());
                });
    }



    private Uni<QuarkusSecurityIdentity> handleJwtToken(JsonWebToken jwt, SessionToken token, String sessionId) {
        long exp = jwt.getExpirationTime();
        if (exp > 0 && exp < Instant.now().getEpochSecond()) {
            LOG.debug("Refresh token expired");
            return refreshToken(token, sessionId)
                    .onItem().transformToUni(refresh -> {
                        try {
                            JsonWebToken newJwt = jwtParser.parse(token.getAccessToken());
                            return buildIdentity(newJwt);
                        } catch (ParseException e) {
                            LOG.debug("Failed to parse JWT token", e);
                            return Uni.createFrom().failure(e);
                        }
                    });
        }

        return buildIdentity(jwt);
    }

    private Uni<QuarkusSecurityIdentity> buildIdentity(JsonWebToken jwt) {
        LOG.debug("JWT Token handled");

        Object preferredUsername = jwt.getClaim("preferred_username");

        return Uni.createFrom().item(() ->
                QuarkusSecurityIdentity.builder()
                        .setPrincipal(preferredUsername::toString)
                        .addAttribute("name", jwt.getClaim("name"))
                        .addAttribute("email", jwt.getClaim("email"))
                        .addRoles(new HashSet<>(jwt.getGroups() != null ? jwt.getGroups() : Collections.emptyList()))
                        .build()
        );
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.contains(path);
    }
}
