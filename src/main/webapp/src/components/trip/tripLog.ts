import dayjs from 'dayjs';
import type { Trip } from '@/generated';

/** a trip of a day in the log, with the kilometers before it that no trip covers */
export interface LogEntry {
  trip: Trip;
  /**
   * kilometers between the finish of the trip before (in time, not deleted)
   * and the start of this one, e.g. a drive nobody logged
   */
  gap?: number;
}

/** the trips of a day, from the morning on */
export interface LogDay {
  /** yyyy-MM-dd in the time zone of the device */
  day: string;
  entries: LogEntry[];
}

/**
 * The trips of a page of the log by day, the latest day first and each day
 * from the morning on, with the gaps of the odometer between the trips. A
 * deleted trip has no gap, and its odometers don't count for the next one.
 *
 * @param trips newest first, as the API lists them
 */
export function logDays(trips: Trip[]): LogDay[] {
  const chronological = [...trips].reverse();
  const gaps = new Map<string, number>();
  let previousEnd: number | undefined;
  for (const trip of chronological) {
    if (trip.deletedAt) continue;
    if (previousEnd !== undefined && trip.startOdometer > previousEnd) {
      gaps.set(trip.id, trip.startOdometer - previousEnd);
    }
    previousEnd = trip.endOdometer ?? undefined;
  }

  const days: LogDay[] = [];
  for (const trip of trips) {
    const day = dayjs(trip.startedAt).format('YYYY-MM-DD');
    const entry = { trip, gap: gaps.get(trip.id) };
    const last = days.at(-1);
    if (last?.day === day) last.entries.unshift(entry);
    else days.push({ day, entries: [entry] });
  }
  return days;
}
