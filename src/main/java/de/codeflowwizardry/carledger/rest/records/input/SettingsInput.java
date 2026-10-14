package de.codeflowwizardry.carledger.rest.records.input;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.codeflowwizardry.carledger.data.UnitSystem;

/**
 * Settings of the own account
 *
 * @param vatCountry
 *            default country for the VAT rate, ISO 3166 alpha-2
 * @param currency
 *            currency of the amounts, ISO 4217
 * @param language
 *            language of the UI, <code>null</code> for the language of the
 *            browser
 * @param unitSystem
 *            units the values are shown and entered in, unchanged without one
 *            (e.g. a client of an older version)
 */
public record SettingsInput(String vatCountry, String currency, String language, UnitSystem unitSystem)
{

	public static final Set<String> LANGUAGES = Set.of("EN", "DE");

	public List<String> validate()
	{
		List<String> errors = new ArrayList<>();
		if (vatCountry == null || !Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2).contains(vatCountry))
		{
			errors.add("VAT country must be an ISO 3166 country code!");
		}
		if (currency == null || Currency.getAvailableCurrencies().stream()
				.noneMatch(c -> c.getCurrencyCode().equals(currency)))
		{
			errors.add("Currency must be an ISO 4217 currency code!");
		}
		if (language != null && !LANGUAGES.contains(language))
		{
			errors.add("Language must be one of " + LANGUAGES + " or empty!");
		}
		return errors;
	}
}
