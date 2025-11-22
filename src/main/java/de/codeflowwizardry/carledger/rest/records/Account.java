package de.codeflowwizardry.carledger.rest.records;

import de.codeflowwizardry.carledger.data.AccountEntity;

public record Account(int maxCars, String name)
{
	public static Account convert(AccountEntity accountEntity)
	{
		return new Account(accountEntity.getMaxCars(), accountEntity.getUserId());
	}
}
