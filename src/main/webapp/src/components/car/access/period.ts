import dayjs, { Dayjs } from 'dayjs';

/**
 * The period of an access in whole days: the app shows the first and the last
 * day, the server keeps the start and the exclusive end
 */
export interface Period {
  validFrom?: string | null;
  validUntil?: string | null;
}

/** @return the start of the first day, from now on without */
export const fromDay = (day: Dayjs | null) =>
  day ? day.startOf('day').toISOString() : undefined;

/** @return the start of the day after the last day, without an end without */
export const untilDay = (day: Dayjs | null) =>
  day ? day.add(1, 'day').startOf('day').toISOString() : undefined;

/** @return the first day of the access */
export const firstDay = (period: Period) =>
  period.validFrom ? dayjs(period.validFrom) : null;

/** @return the last day of the access, the end is exclusive */
export const lastDay = (period: Period) =>
  period.validUntil ? dayjs(period.validUntil).subtract(1, 'ms') : null;

/** @return whether the access has ended, it no longer counts */
export const hasEnded = (period: Period, now = dayjs()) =>
  !!period.validUntil && !dayjs(period.validUntil).isAfter(now);
