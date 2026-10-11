import { useCallback, useState } from 'react';
import countryVat from 'country-vat';

const STORAGE_KEY = 'vatCountry';
const DEFAULT_COUNTRY = 'DE';

function readStored() {
  try {
    return localStorage.getItem(STORAGE_KEY) ?? DEFAULT_COUNTRY;
  } catch {
    return DEFAULT_COUNTRY;
  }
}

/**
 * Remembers the last used VAT country and exposes its rate in percent, as the API expects it.
 */
export default function useVatCountry() {
  const [country, setCountryState] = useState(readStored);

  const setCountry = useCallback((code: string) => {
    setCountryState(code);
    try {
      localStorage.setItem(STORAGE_KEY, code);
    } catch {
      // storage not available
    }
  }, []);

  const rate = countryVat(country);
  const vatRate = rate === null ? undefined : Math.round(rate * 100);

  return { country, setCountry, vatRate };
}
