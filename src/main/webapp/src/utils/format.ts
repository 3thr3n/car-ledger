import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { getMyselfOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';

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

  return useMemo(() => {
    const number = (value?: number | null, digits = 2) =>
      isMissing(value)
        ? EMPTY
        : new Intl.NumberFormat(locale, {
            minimumFractionDigits: digits,
            maximumFractionDigits: digits,
          }).format(num(value)!);

    return {
      locale,
      currency,
      currencySymbol: currencySymbol(locale, currency),
      number,
      money: (value, digits = 2) =>
        isMissing(value)
          ? EMPTY
          : new Intl.NumberFormat(locale, {
              style: 'currency',
              currency,
              minimumFractionDigits: digits,
              maximumFractionDigits: digits,
            }).format(num(value)!),
      unit: (value, unit, digits = 2) =>
        isMissing(value) ? EMPTY : `${number(value, digits)} ${unit}`,
      date: (value) =>
        value
          ? new Intl.DateTimeFormat(locale, { dateStyle: 'medium' }).format(
              dayjs(value).toDate(),
            )
          : EMPTY,
    };
  }, [locale, currency]);
}

export function toApiDate(value: dayjs.Dayjs | null | undefined) {
  return value ? value.format('YYYY-MM-DD') : undefined;
}
