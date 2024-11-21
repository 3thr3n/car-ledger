package de.codeflowwizardry.carledger.rest;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.rest.records.AccountPojo;
import io.quarkus.security.Authenticated;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Authenticated
@Path("user")
public class AccountResource extends AbstractResource
{
	private final static Logger LOG = LoggerFactory.getLogger(AccountResource.class);

	@GET
	@Path("me")
	@Operation(operationId = "getMyself")
	@APIResponse(responseCode = "200", description = "User found.")
	public AccountPojo getMe()
	{
		return AccountPojo.convert(checkUser(context.getName()));
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
		}

		return account;
	}
}
