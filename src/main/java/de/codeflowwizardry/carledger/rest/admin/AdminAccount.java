package de.codeflowwizardry.carledger.rest.admin;

import java.time.OffsetDateTime;

import de.codeflowwizardry.carledger.data.repository.AccountRepository.AccountUsage;

/**
 * Account as seen by an administrator
 *
 * @param lastSeen
 *            last request of the user, accurate to an hour
 */
public record AdminAccount(long id, String name, int maxCars, long cars, long bills, OffsetDateTime createdAt,
		OffsetDateTime lastSeen)
{
	public static AdminAccount convert(AccountUsage usage)
	{
		var account = usage.account();
		return new AdminAccount(account.getId(), account.getUserId(), account.getMaxCars(), usage.cars(),
				usage.bills(), account.getCreatedAt(), account.getLastSeen());
	}
}
