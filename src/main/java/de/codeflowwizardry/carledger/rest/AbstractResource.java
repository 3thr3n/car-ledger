package de.codeflowwizardry.carledger.rest;

import de.codeflowwizardry.carledger.data.Account;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import io.quarkus.security.identity.CurrentIdentityAssociation;

import javax.security.auth.login.AccountNotFoundException;
import java.util.Optional;

public abstract class AbstractResource
{
	protected final CurrentIdentityAssociation context;
	protected final AccountRepository accountRepository;

	protected AbstractResource(CurrentIdentityAssociation context, AccountRepository accountRepository)
	{
		this.context = context;
		this.accountRepository = accountRepository;
	}

	protected Account getAccount() throws AccountNotFoundException {
		Optional<Account> account = accountRepository.findByIdentifier(getName());
		if (account.isEmpty()) {
			throw new AccountNotFoundException();
		}

		return account.get();
	}

	protected String getName() {
		return context.getIdentity().getPrincipal().getName();
	}
}
