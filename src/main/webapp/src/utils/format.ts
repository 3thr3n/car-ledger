import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import dayjs from 'dayjs';

const localeMap: Record<string, string> = {
  EN: 'en-US',
  DE: 'de-DE',
};

export interface Formatter {
  locale: string;
  /** Euro amount, e.g. "12.30 €" */
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
 * The API serializes decimals as strings ("12.30"), even though the spec says number
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

export function useFormat(): Formatter {
  const { i18n } = useTranslation();
  const locale = localeMap[i18n.language] ?? i18n.language ?? 'en-US';

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
      number,
      money: (value, digits = 2) =>
        isMissing(value)
          ? EMPTY
          : new Intl.NumberFormat(locale, {
              style: 'currency',
              currency: 'EUR',
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
  }, [locale]);
}

export function toApiDate(value: dayjs.Dayjs | null | undefined) {
  return value ? value.format('YYYY-MM-DD') : undefined;
}
