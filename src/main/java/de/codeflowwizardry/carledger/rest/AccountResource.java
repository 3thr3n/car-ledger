package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.rest.records.AccountPojo;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Authenticated
@Path("user")
@ApplicationScoped
public class AccountResource extends AbstractResource
{
	/**
	 * CDI proxying
	 */
	public AccountResource()
	{
		super(null, null);
	}

	@Inject
	public AccountResource(CurrentIdentityAssociation securityIdentity, AccountRepository accountRepository)
	{
		super(securityIdentity, accountRepository);
	}

	@GET
	@Path("me")
	@Operation(operationId = "getMyself")
	@APIResponse(responseCode = "200", description = "User found.")
	public AccountPojo getMe()
	{
		String name = getName();
		Account account = accountRepository.findByIdentifier(name);
		return AccountPojo.convert(account);
	}
}
