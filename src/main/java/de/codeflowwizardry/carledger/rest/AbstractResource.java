package de.codeflowwizardry.carledger.rest;

import java.security.Principal;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import jakarta.inject.Inject;

public abstract class AbstractResource
{
	@Inject
	protected Principal context;

	@Inject
	protected AccountRepository accountRepository;

	protected Account getAccount()
	{
		return accountRepository.findByIdentifier(context.getName());
	}
}
