package de.codeflowwizardry.carledger.rest;

import io.quarkus.security.identity.SecurityIdentity;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.records.Account;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import org.eclipse.microprofile.jwt.JsonWebToken;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.notification.MailConfig;
import de.codeflowwizardry.carledger.rest.records.NotificationSettings;
import de.codeflowwizardry.carledger.rest.records.input.NotificationSettingsInput;
import de.codeflowwizardry.carledger.rest.records.input.SettingsInput;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("user")
public class AccountResource extends AbstractResource
{
	private static final Logger LOG = LoggerFactory.getLogger(AccountResource.class);

	private final MailConfig mailConfig;

	@Inject
	public AccountResource(SecurityIdentity identity, AccountRepository accountRepository, MailConfig mailConfig)
	{
		super(identity, accountRepository);
		this.mailConfig = mailConfig;
	}

	/**
	 * "last seen" is only written once in this period, the frontend asks every
	 * few minutes
	 */
	private static final Duration SEEN_PRECISION = Duration.ofHours(1);

	@GET
	@Path("me")
	@Transactional
	@Operation(operationId = "getMyself")
	@APIResponse(responseCode = "200", description = "User found.")
	public Account getMe(@Context HttpHeaders headers)
	{
		checkUser(context.getName());
		// loaded again, a newly created account belongs to another transaction
		AccountEntity account = accountRepository.findByIdentifier(context.getName());

		OffsetDateTime now = OffsetDateTime.now();
		if (account.getLastSeen() == null || account.getLastSeen().isBefore(now.minus(SEEN_PRECISION)))
		{
			account.setLastSeen(now);
		}
		remember(account, headers);

		return Account.convert(account, isAdmin());
	}

	/**
	 * What the notifications need without a login: the address and the role of
	 * the token, the language of the browser. Only written when changed.
	 */
	private void remember(AccountEntity account, HttpHeaders headers)
	{
		if (identity.getPrincipal() instanceof JsonWebToken token)
		{
			String email = token.getClaim("email");
			email = email == null || email.isBlank() ? null : email.trim();
			if (!Objects.equals(email, account.getEmail()))
			{
				account.setEmail(email);
			}
		}
		if (account.isAdmin() != isAdmin())
		{
			account.setAdmin(isAdmin());
		}
		String language = browserLanguage(headers);
		if (language != null && !language.equals(account.getBrowserLanguage()))
		{
			account.setBrowserLanguage(language);
		}
	}

	/**
	 * @return the first language of the browser that the app knows, EN or DE
	 */
	static String browserLanguage(HttpHeaders headers)
	{
		if (headers == null)
		{
			return null;
		}
		return headers.getAcceptableLanguages().stream()
				.map(locale -> locale.getLanguage().toUpperCase(Locale.ROOT))
				.filter(SettingsInput.LANGUAGES::contains)
				.findFirst()
				.orElse(null);
	}

	@GET
	@Path("me/notifications")
	@Transactional
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "getMyNotifications", description = "Notifications of the own account by e-mail")
	@APIResponse(responseCode = "200", description = "Notification settings found.")
	public NotificationSettings getNotifications()
	{
		return NotificationSettings.convert(getAccount(), mailConfig.enabled());
	}

	@PUT
	@Path("me/notifications")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateMyNotifications", description = "Turns the notifications of the own account by e-mail on or off")
	@APIResponse(responseCode = "200", description = "Notification settings saved.")
	public NotificationSettings updateNotifications(NotificationSettingsInput input)
	{
		AccountEntity account = getAccount();
		if (input != null && input.newRequests() != null)
		{
			account.setNotifyNewRequests(input.newRequests());
		}
		if (input != null && input.requestDecided() != null)
		{
			account.setNotifyRequestDecided(input.requestDecided());
		}
		if (input != null && input.reminders() != null)
		{
			account.setNotifyReminders(input.reminders());
		}
		return NotificationSettings.convert(account, mailConfig.enabled());
	}

	@PUT
	@Path("me/settings")
	@Transactional
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateMySettings", description = "Updates the settings of the own account")
	@APIResponse(responseCode = "200", description = "Settings saved.")
	@APIResponse(responseCode = "400", description = "Validation failed, please check the response", content = @Content(schema = @Schema(implementation = List.class)))
	public Account updateSettings(SettingsInput input)
	{
		Utils.requireValid(input.validate());

		AccountEntity account = accountRepository.findByIdentifier(context.getName());
		account.setVatCountry(input.vatCountry());
		account.setCurrency(input.currency());
		account.setLanguage(input.language());
		if (input.unitSystem() != null)
		{
			account.setUnitSystem(input.unitSystem());
		}

		return Account.convert(account, isAdmin());
	}

	@DELETE
	@Path("me")
	@Operation(operationId = "deleteMyself", description = "Deletes the own account with all its cars, entries, recurring costs and reminders. The account at the identity provider stays.")
	@APIResponse(responseCode = "204", description = "Account deleted.")
	public Response deleteMe()
	{
		Optional<AccountEntity> account = accountRepository.find("userId", context.getName()).singleResultOptional();
		account.ifPresent(a -> {
			LOG.info("Account {} deleted by its owner", a.getId());
			accountRepository.deleteAccount(a.getId());
		});
		return Response.noContent().build();
	}

	private boolean isAdmin()
	{
		return identity.hasRole("admin");
	}

	public AccountEntity checkUser(String name)
	{
		ObjectUtils.requireNonEmpty(name);

		AccountEntity accountEntity;

		try
		{
			LOG.debug("checking user {}...", name);
			accountEntity = accountRepository.findByIdentifier(name);
			LOG.debug("user exists!");
		}
		catch (BadRequestException e)
		{
			LOG.debug("user does not exist! creating {}...", name);
			try
			{
				accountEntity = accountRepository.createAccount(name, 1);
				LOG.debug("user created!");
			}
			catch (ConstraintViolationException cve)
			{
				LOG.debug("Duplicated request on first login");
				accountEntity = accountRepository.findByIdentifier(name);
			}
		}

		return accountEntity;
	}
}
