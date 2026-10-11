package de.codeflowwizardry.carledger.rest;

import java.security.Principal;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import jakarta.inject.Inject;

public abstract class AbstractResource
{
	public static final int MAX_PAGE_SIZE = 100;

	protected final Principal context;
	protected final AccountRepository accountRepository;

	@Inject
	protected AbstractResource(Principal context, AccountRepository accountRepository)
	{
		this.context = context;
		this.accountRepository = accountRepository;
	}

	/**
	 * @return the requested page, pages start at 1
	 */
	protected static int toPageNumber(int page)
	{
		return Math.max(page, 1);
	}

	/**
	 * @return the requested page size, limited to 1 - {@value #MAX_PAGE_SIZE}
	 */
	protected static int toPageSize(int size)
	{
		return Math.clamp(size, 1, MAX_PAGE_SIZE);
	}

	protected AccountEntity getAccount()
	{
		return accountRepository.findByIdentifier(context.getName());
	}
}
