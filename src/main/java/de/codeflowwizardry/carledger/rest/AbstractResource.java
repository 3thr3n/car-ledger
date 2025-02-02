package de.codeflowwizardry.carledger.rest;

import java.security.Principal;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import jakarta.inject.Inject;

public abstract class AbstractResource
{
	protected final Principal context;
	protected final AccountRepository accountRepository;

	@Inject
	protected AbstractResource(Principal context, AccountRepository accountRepository)
	{
		this.context = context;
		this.accountRepository = accountRepository;
	}

	protected Account getAccount()
	{
		return accountRepository.findByIdentifier(context.getName());
	}
}
