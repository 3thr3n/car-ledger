import vatRates from 'country-vat/src/rates.json';

type VatRateMap = Record<string, number>;

export const supportedVatCountries: VatRateMap = Object.entries(vatRates)
  .filter(([code, rate]) => code.length === 2 && typeof rate === 'number')
  .reduce<VatRateMap>((acc, [code, rate]) => {
    if (typeof rate === 'number') {
      acc[code] = rate;
    }
    return acc;
  }, {});

export function formatVatPercent(rate: number): number {
  return Math.round(rate * 10000) / 100;
}
