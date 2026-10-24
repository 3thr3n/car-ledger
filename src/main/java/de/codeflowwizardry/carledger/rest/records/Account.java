package de.codeflowwizardry.carledger.rest.records;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import de.codeflowwizardry.carledger.data.Accent;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.UnitSystem;

/**
 * @param admin
 *            whether the user may use the administration
 * @param language
 *            language of the UI, <code>null</code> for the language of the
 *            browser
 * @param unitSystem
 *            units the values are shown and entered in, the API always uses km
 *            and liters
 * @param accent
 *            accent color of the UI
 */
public record Account(int maxCars, String name, boolean admin, String vatCountry, String currency, String language,
		@Schema(required = true) UnitSystem unitSystem, @Schema(required = true) Accent accent)
{
	public static Account convert(AccountEntity accountEntity, boolean admin)
	{
		return new Account(accountEntity.getMaxCars(), accountEntity.getUserId(), admin,
				accountEntity.getVatCountry(), accountEntity.getCurrency(), accountEntity.getLanguage(),
				accountEntity.getUnitSystem(), accountEntity.getAccent());
	}
}
