package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.records.AccountPojo;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.security.Principal;

@Path("user")
public class AccountResource extends AbstractResource
{
	@Inject
	public AccountResource(Principal principal, AccountRepository accountRepository)
	{
		super(principal, accountRepository);
	}

	@GET
	@Path("me")
	@Operation(operationId = "getMyself")
	@APIResponse(responseCode = "200", description = "User found.")
	public AccountPojo getMe()
	{
		return AccountPojo.convert(accountRepository.findByIdentifier(context.getName()));
	}
}
