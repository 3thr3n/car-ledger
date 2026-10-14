import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { getMyselfOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { convert, Units, unitsOf } from '@/utils/units';

export const DEFAULT_CURRENCY = 'EUR';

const localeMap: Record<string, string> = {
  EN: 'en-US',
  DE: 'de-DE',
};

export interface Formatter {
  locale: string;
  /** currency of the user, ISO 4217 */
  currency: string;
  /** e.g. "€" */
  currencySymbol: string;
  /** Amount in the currency of the user, e.g. "€12.30" */
  money: (value?: number | null, digits?: number) => string;
  /** Plain number with fixed fraction digits */
  number: (value?: number | null, digits?: number) => string;
  /** Number with unit suffix, e.g. "6.4 l/100km" */
  unit: (
    value: number | null | undefined,
    unit: string,
    digits?: number,
  ) => string;
  /** Date from the API (yyyy-MM-dd) */
  date: (value?: string | null) => string;
  /** units of the account, the API always uses km, liters and ct/l */
  units: Units;
  /** km of the API, e.g. "12,400 km" or "7,705 mi" */
  distance: (km?: number | null, digits?: number) => string;
  /** liters of the API, e.g. "41.02 l" or "10.84 gal" */
  volume: (liters?: number | null, digits?: number) => string;
  /** cent per liter of the API, e.g. "172.9 ct/l" or "$3.459/gal" */
  fuelPrice: (centsPerLiter?: number | null) => string;
  /** l/100km of the API, e.g. "6.40 l/100km" or "36.8 mpg" */
  consumption: (litersPer100Km?: number | null) => string;
  /** kWh/100km of the API, e.g. "17.40 kWh/100km" or "28.00 kWh/100mi" */
  chargingConsumption: (kwhPer100Km?: number | null) => string;
  /** cent per km of the API, e.g. "9.50 ct/km" or "15.29 ct/mi" */
  costPerDistance: (centsPerKm?: number | null) => string;
}

export const EMPTY = '—';

/**
 * Number of an API value, undefined when missing
 */
export function num(value?: number | string | null): number | undefined {
  if (value === null || value === undefined || value === '') return undefined;
  const parsed = Number(value);
  return Number.isNaN(parsed) ? undefined : parsed;
}

/** set and not zero */
export function hasValue(value?: number | string | null) {
  return !!num(value);
}

function isMissing(value?: number | null): value is null | undefined {
  return num(value) === undefined;
}

/**
 * Symbol of a currency in a locale, the code itself if there is none
 */
export function currencySymbol(locale: string, currency: string) {
  try {
    return (
      new Intl.NumberFormat(locale, { style: 'currency', currency })
        .formatToParts(0)
        .find((part) => part.type === 'currency')?.value ?? currency
    );
  } catch {
    return currency;
  }
}

export function useFormat(): Formatter {
  const { i18n } = useTranslation();
  const locale = localeMap[i18n.language] ?? i18n.language ?? 'en-US';
  // the account is loaded by the app shell already, this only reads it
  const { data: me } = useQuery({
    ...getMyselfOptions({ client: localClient }),
    enabled: false,
  });
  const currency = me?.currency ?? DEFAULT_CURRENCY;
  const unitSystem = me?.unitSystem;

  return useMemo(() => {
    const number = (value?: number | null, digits = 2) =>
      isMissing(value)
        ? EMPTY
        : new Intl.NumberFormat(locale, {
            minimumFractionDigits: digits,
            maximumFractionDigits: digits,
          }).format(num(value)!);

    const money = (value?: number | null, digits = 2) =>
      isMissing(value)
        ? EMPTY
        : new Intl.NumberFormat(locale, {
            style: 'currency',
            currency,
            minimumFractionDigits: digits,
            maximumFractionDigits: digits,
          }).format(num(value)!);
    const unit = (
      value: number | null | undefined,
      label: string,
      digits = 2,
    ) => (isMissing(value) ? EMPTY : `${number(value, digits)} ${label}`);
    const units = unitsOf(unitSystem);
    // the API sends the numbers as numbers, the strings of older values too
    const api = (value?: number | null) => num(value);

    return {
      locale,
      currency,
      currencySymbol: currencySymbol(locale, currency),
      number,
      money,
      unit,
      units,
      distance: (km, digits = 0) =>
        unit(
          convert(api(km), units.distance.fromKm),
          units.distance.label,
          digits,
        ),
      volume: (liters, digits = 2) =>
        unit(
          convert(api(liters), units.volume.fromLiters),
          units.volume.label,
          digits,
        ),
      fuelPrice: (centsPerLiter) => {
        const value = convert(
          api(centsPerLiter),
          units.fuelPrice.fromCentsPerLiter,
        );
        return units.fuelPrice.inCurrency
          ? isMissing(value)
            ? EMPTY
            : `${money(value, units.fuelPrice.digits)}/${units.volume.label}`
          : unit(value, `ct/${units.volume.label}`, units.fuelPrice.digits);
      },
      consumption: (litersPer100Km) =>
        unit(
          convert(api(litersPer100Km), units.consumption.fromLitersPer100Km),
          units.consumption.label,
          units.consumption.digits,
        ),
      chargingConsumption: (kwhPer100Km) =>
        unit(
          convert(api(kwhPer100Km), units.chargingConsumption.fromKwhPer100Km),
          units.chargingConsumption.label,
        ),
      costPerDistance: (centsPerKm) =>
        unit(
          convert(api(centsPerKm), units.costPerDistance.fromCentsPerKm),
          units.costPerDistance.label,
        ),
      date: (value) =>
        value
          ? new Intl.DateTimeFormat(locale, { dateStyle: 'medium' }).format(
              dayjs(value).toDate(),
            )
          : EMPTY,
    };
  }, [locale, currency, unitSystem]);
}

export function toApiDate(value: dayjs.Dayjs | null | undefined) {
  return value ? value.format('YYYY-MM-DD') : undefined;
}
