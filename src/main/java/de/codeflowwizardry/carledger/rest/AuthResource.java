package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.client.KeycloakTokenResponse;
import de.codeflowwizardry.carledger.client.KeycloakTokenService;
import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.records.Credentials;
import de.codeflowwizardry.carledger.state.SessionManager;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static java.net.URLEncoder.encode;

@Path("auth")
@ApplicationScoped
public class AuthResource {
    private final static Logger LOG = LoggerFactory.getLogger(AuthResource.class);

    private final AccountRepository accountRepository;
    private final KeycloakTokenService keycloakTokenService;
    private final SessionManager sessionManager;

    @ConfigProperty(name = "keycloak.client-id")
    String clientId;

    @ConfigProperty(name = "keycloak.client-secret")
    String clientSecret;

    @ConfigProperty(name = "keycloak.realm.url")
    String realmUrl;

    @ConfigProperty(name = "redirect_uri")
    String redirectUri;

    @Inject
    public AuthResource(AccountRepository accountRepository, @RestClient KeycloakTokenService keycloakTokenService, SessionManager sessionManager) {
        this.accountRepository = accountRepository;
        this.keycloakTokenService = keycloakTokenService;
        this.sessionManager = sessionManager;
    }

    @POST
    @Path("login")
    @Operation(operationId = "login", description = "Here should the browser redirect, when 'login' is pressed")
    public Response login(Credentials credentials, @QueryParam("method") String method) {
        Response response = handleMethod(method);
        if (response != null) {
            return response;
        }

        if (credentials == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        KeycloakTokenResponse tokenResponse = keycloakTokenService.getToken(
                "password",
                clientId,
                clientSecret,
                credentials.username(),
                credentials.password()
        );

        Response handled = handleTokenResponse(tokenResponse);

        checkUser(credentials.username());

        return handled;
    }

    private Response handleTokenResponse(KeycloakTokenResponse tokenResponse) {
        String sessionId = UUID.randomUUID().toString();

        sessionManager.store(sessionId, tokenResponse);

        NewCookie cookie = new NewCookie.Builder("SESSION_ID")
                .value(sessionId)
                .path("/")
                .maxAge(-1)
                .secure(true)
                .httpOnly(true)
                .build();

        return Response.ok().cookie(cookie).build();
    }

    private Response handleMethod(String method) {
        if (StringUtils.isEmpty(method)) {
            return null;
        }

        String builder = realmUrl +
                "/protocol/openid-connect/token" +
                "?client_id=" + encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_type=" + "code" +
                "&scope=" + "openid" +
                "&kc_idp_hint=" + method;

        return Response.seeOther(URI.create(builder)).build();
    }

    @GET
    @Path("callback")
    public Response callback(@QueryParam("code") String code) {
        KeycloakTokenResponse tokenResponse = keycloakTokenService.processCode(
                "authorization_code",
                code,
                clientId,
                clientSecret,
                redirectUri
        );

        return handleTokenResponse(tokenResponse);
    }

    @GET
    @Path("log")
    @Authenticated
    public String test() {
        LOG.info("I should be logged in");
        return "LoggedIn!";
    }

    @POST
    @Path("logout")
    @Operation(operationId = "logout", description = "Logout current user")
    public Response logout(@CookieParam("SESSION_ID") String sessionId) {
        NewCookie expiredCookie = new NewCookie.Builder("SESSION_ID")
                .value("")
                .path("/")
                .maxAge(0)
                .secure(true)
                .httpOnly(true)
                .build();

        if (sessionId != null) {
            sessionManager.remove(sessionId);
        }

        return Response.ok().cookie(expiredCookie).build();
    }

    @GET
    @Path("/register")
    public Response registerRedirect() {
        String registrationUrl = realmUrl + "protocol/openid-connect/registrations?client_id=" + clientId;
        return Response.seeOther(URI.create(registrationUrl)).build();
    }

    private void checkUser(String name) {
        ObjectUtils.requireNonEmpty(name);

        try {
            LOG.info("checking user {}...", name);
            Optional<Account> optionalAccount = accountRepository.findByIdentifier(name);
            if (optionalAccount.isEmpty()) {
                createUser(name);
            } else {
                LOG.info("user exists!");
            }
        } catch (Exception e) {
            LOG.warn("failed to check user {}!", name, e);
            throw new BadRequestException();
        }
    }

    @Transactional
    public void createUser(String name) {
        LOG.info("user does not exist! creating {}...", name);
        Account account = new Account();
        account.setUserId(name);
        account.setMaxCars(1);
        accountRepository.persist(account);
        LOG.info("user created!");
    }
}
