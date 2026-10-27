import dayjs, { Dayjs } from 'dayjs';
import { toApiDate } from '@/utils/format';

/** the shortcuts for the period of the map, all within the 31 days of the server */
export const SHORTCUTS = ['today', 'last3', 'last7', 'month'] as const;
export type Shortcut = (typeof SHORTCUTS)[number];

export interface Range {
  from?: string;
  to?: string;
}

/** the period of the shortcut, up to today */
export function rangeOf(shortcut: Shortcut, today: Dayjs = dayjs()): Range {
  const from = {
    today: today,
    last3: today.subtract(2, 'day'),
    last7: today.subtract(6, 'day'),
    month: today.startOf('month'),
  }[shortcut];
  return { from: toApiDate(from), to: toApiDate(today) };
}

/**
 * @return the shortcut of the period, the first of equal ones (on the 1st,
 *         "today" before "this month"); none for one chosen by hand
 */
export function shortcutOf(
  range: Range,
  today: Dayjs = dayjs(),
): Shortcut | undefined {
  return SHORTCUTS.find((shortcut) => {
    const of = rangeOf(shortcut, today);
    return of.from === range.from && of.to === range.to;
  });
}
