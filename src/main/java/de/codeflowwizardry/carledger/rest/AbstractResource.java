package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import io.quarkus.security.identity.CurrentIdentityAssociation;

public abstract class AbstractResource
{
	protected final CurrentIdentityAssociation context;
	protected final AccountRepository accountRepository;

	protected AbstractResource(CurrentIdentityAssociation context, AccountRepository accountRepository)
	{
		this.context = context;
		this.accountRepository = accountRepository;
	}

	protected Account getAccount()
	{
		return accountRepository.findByIdentifier(getName());
	}

	protected String getName()
	{
		return context.getIdentity().getPrincipal().getName();
	}
}
