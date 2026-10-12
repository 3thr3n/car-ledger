package de.codeflowwizardry.carledger.rest.records;

import de.codeflowwizardry.carledger.data.AccountEntity;

/**
 * @param admin
 *            whether the user may use the administration
 * @param language
 *            language of the UI, <code>null</code> for the language of the
 *            browser
 */
public record Account(int maxCars, String name, boolean admin, String vatCountry, String currency, String language)
{
	public static Account convert(AccountEntity accountEntity, boolean admin)
	{
		return new Account(accountEntity.getMaxCars(), accountEntity.getUserId(), admin,
				accountEntity.getVatCountry(), accountEntity.getCurrency(), accountEntity.getLanguage());
	}
}
