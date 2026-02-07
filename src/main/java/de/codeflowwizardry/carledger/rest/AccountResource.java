package de.codeflowwizardry.carledger.rest;

import java.security.Principal;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.records.Account;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("user")
public class AccountResource extends AbstractResource
{
	private static final Logger LOG = LoggerFactory.getLogger(AccountResource.class);

	@Inject
	public AccountResource(Principal context, AccountRepository accountRepository)
	{
		super(context, accountRepository);
	}

	@GET
	@Path("me")
	@Operation(operationId = "getMyself")
	@APIResponse(responseCode = "200", description = "User found.")
	public Account getMe()
	{
		return Account.convert(checkUser(context.getName()));
	}

	@Transactional
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
			accountEntity = new AccountEntity();
			accountEntity.setUserId(name);
			accountEntity.setMaxCars(3);
			accountRepository.persist(accountEntity);
			LOG.debug("user created!");
		}
		catch (ConstraintViolationException e)
		{
			LOG.debug("Duplicated request on first login");
			accountEntity = accountRepository.findByIdentifier(name);
		}

		return accountEntity;
	}
}
