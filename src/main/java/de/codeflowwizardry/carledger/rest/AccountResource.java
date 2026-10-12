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

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.rest.records.input.SettingsInput;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("user")
public class AccountResource extends AbstractResource
{
	private static final Logger LOG = LoggerFactory.getLogger(AccountResource.class);

	@Inject
	public AccountResource(SecurityIdentity identity, AccountRepository accountRepository)
	{
		super(identity, accountRepository);
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
	public Account getMe()
	{
		checkUser(context.getName());
		// loaded again, a newly created account belongs to another transaction
		AccountEntity account = accountRepository.findByIdentifier(context.getName());

		OffsetDateTime now = OffsetDateTime.now();
		if (account.getLastSeen() == null || account.getLastSeen().isBefore(now.minus(SEEN_PRECISION)))
		{
			account.setLastSeen(now);
		}

		return Account.convert(account, isAdmin());
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

		return Account.convert(account, isAdmin());
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
