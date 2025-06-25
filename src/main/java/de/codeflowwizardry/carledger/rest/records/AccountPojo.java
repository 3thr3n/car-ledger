package de.codeflowwizardry.carledger.rest.records;

import de.codeflowwizardry.carledger.data.Account;

public record AccountPojo(int maxCars,String name){public static AccountPojo convert(Account account){return new AccountPojo(account.getMaxCars(),account.getUserId());}}
