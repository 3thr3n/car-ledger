import { useCallback, useState } from 'react';
import countryVat from 'country-vat';
import { supportedVatCountries } from '@/utils/VatCountries';

const STORAGE_KEY = 'vatCountry';
const DEFAULT_COUNTRY = 'DE';

function readStored() {
  try {
    return localStorage.getItem(STORAGE_KEY) ?? DEFAULT_COUNTRY;
  } catch {
    return DEFAULT_COUNTRY;
  }
}

/** VAT rate in percent, as the API expects it */
function rateOf(country: string) {
  const rate = countryVat(country);
  return rate === null ? undefined : Math.round(rate * 100);
}

/**
 * Country for a VAT rate of an existing bill: the remembered country if it fits,
 * otherwise the first one with that rate, '' if none has it.
 */
function countryFor(vatRate: number) {
  const stored = readStored();
  if (rateOf(stored) === vatRate) return stored;
  return (
    Object.keys(supportedVatCountries)
      .sort()
      .find((code) => rateOf(code) === vatRate) ?? ''
  );
}

/**
 * Remembers the last used VAT country and exposes its rate in percent, as the API expects it.
 *
 * @param initialRate rate of an existing bill, which is kept when no country has it
 */
export default function useVatCountry(initialRate?: number) {
  const [country, setCountryState] = useState(() =>
    initialRate === undefined ? readStored() : countryFor(initialRate),
  );

  const setCountry = useCallback((code: string) => {
    setCountryState(code);
    try {
      localStorage.setItem(STORAGE_KEY, code);
    } catch {
      // storage not available
    }
  }, []);

  const vatRate = country ? rateOf(country) : initialRate;

  return { country, setCountry, vatRate };
}
