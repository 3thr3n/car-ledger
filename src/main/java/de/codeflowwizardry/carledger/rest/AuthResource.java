package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.client.KeycloakTokenResponse;
import de.codeflowwizardry.carledger.client.KeycloakTokenService;
import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.records.Credentials;
import de.codeflowwizardry.carledger.state.SessionManager;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Path("auth")
public class AuthResource
{
	private final static Logger LOG = LoggerFactory.getLogger(AuthResource.class);

    private final AccountRepository accountRepository;
	private final KeycloakTokenService keycloakTokenService;
	private final SessionManager sessionManager;

	@ConfigProperty(name = "redirect.to")
	String redirectTo;

	@ConfigProperty(name = "keycloak.client-id")
	String clientId;

	@ConfigProperty(name = "keycloak.client-secret")
	String clientSecret;

	@Inject
	public AuthResource(AccountRepository accountRepository, @RestClient KeycloakTokenService keycloakTokenService, SessionManager sessionManager)
	{
        this.accountRepository = accountRepository;
        this.keycloakTokenService = keycloakTokenService;
        this.sessionManager = sessionManager;
    }

	@POST
	@Path("login")
	@Operation(operationId = "login", description = "Here should the browser redirect, when 'login' is pressed")
	public Response login(Credentials credentials)
	{

		KeycloakTokenResponse tokenResponse = keycloakTokenService.getToken(
				"password",
				clientId,
				clientSecret,
				credentials.username(),
				credentials.password()
		);

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

	@POST
	@Path("logout")
	@Operation(operationId = "logout", description = "Logout current user")
	public Response logout(@CookieParam("SESSION_ID") String sessionId)
	{
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

	@Transactional
	public Account checkUser(String name)
	{
		ObjectUtils.requireNonEmpty(name);

		Account account;

		try
		{
			LOG.info("checking user {}...", name);
			account = accountRepository.findByIdentifier(name);
			LOG.info("user exists!");
		}
		catch (BadRequestException e)
		{
			LOG.info("user does not exist! creating {}...", name);
			account = new Account();
			account.setUserId(name);
			account.setMaxCars(1);
			accountRepository.persist(account);
			LOG.info("user created!");
		} catch (Exception e) {
			LOG.warn("failed to check user {}!", name, e);
			throw new BadRequestException();
		}
		return account;
	}
}
