import { useState } from 'react';
import countryVat from 'country-vat';
import { supportedVatCountries } from '@/utils/VatCountries';
import useMe from '@/hooks/useMe';

const DEFAULT_COUNTRY = 'DE';

/** VAT rate in percent, as the API expects it */
export function rateOf(country: string) {
  const rate = countryVat(country);
  return rate === null ? undefined : Math.round(rate * 100);
}

/**
 * Country for a VAT rate of an existing bill: the preferred country if it fits,
 * otherwise the first one with that rate, '' if none has it.
 */
export function countryFor(vatRate: number, preferred: string) {
  if (rateOf(preferred) === vatRate) return preferred;
  return (
    Object.keys(supportedVatCountries)
      .sort()
      .find((code) => rateOf(code) === vatRate) ?? ''
  );
}

/**
 * VAT country of a bill form, the default comes from the settings of the account.
 * Exposes the rate in percent, as the API expects it.
 *
 * @param initialRate rate of an existing bill, which is kept when no country has it
 */
export default function useVatCountry(initialRate?: number) {
  const { account } = useMe();
  const preferred = account?.vatCountry ?? DEFAULT_COUNTRY;
  // undefined until the user picks one, so the loaded settings apply
  const [picked, setCountry] = useState<string>();

  const country =
    picked ??
    (initialRate === undefined
      ? preferred
      : countryFor(initialRate, preferred));
  const vatRate = country ? rateOf(country) : initialRate;

  return { country, setCountry, vatRate };
}
