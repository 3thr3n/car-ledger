import { describe, expect, it } from 'vitest';
import { Trip, TripType } from '@/generated';
import { logDays } from './tripLog';

const trip = (
  id: string,
  startedAt: string,
  startOdometer: number,
  endOdometer?: number,
  deletedAt?: string,
): Trip => ({
  id,
  carId: 1,
  type: TripType.COMMUTE,
  startOdometer,
  endOdometer,
  startedAt,
  restored: false,
  enteredLater: false,
  createdAt: startedAt,
  deletedAt,
});

/** at noon, so the local day is the same in every time zone */
const MORNING = trip('a', '2026-10-21T10:00:00Z', 100, 118);
const NOON = trip('b', '2026-10-21T12:00:00Z', 118, 130);
const NEXT_DAY = trip('c', '2026-10-22T12:00:00Z', 172, 190);

describe('trip log', () => {
  it('lists the latest day first, each from the morning on', () => {
    const days = logDays([NEXT_DAY, NOON, MORNING]);
    expect(days.map((day) => day.day)).toEqual(['2026-10-22', '2026-10-21']);
    expect(days[1].entries.map((entry) => entry.trip.id)).toEqual(['a', 'b']);
  });

  it('finds the kilometers no trip covers', () => {
    const days = logDays([NEXT_DAY, NOON, MORNING]);
    expect(days[0].entries[0].gap).toBe(42);
    expect(days[1].entries.map((entry) => entry.gap)).toEqual([
      undefined,
      undefined,
    ]);
  });

  it('leaves out the odometers of a deleted trip', () => {
    const deleted = trip('d', '2026-10-21T14:00:00Z', 130, 150, '2026-10-23');
    const days = logDays([NEXT_DAY, deleted, NOON, MORNING]);
    // from 130 of the trip at noon, not 150 of the deleted one
    expect(days[0].entries[0].gap).toBe(42);
    expect(days[1].entries.find((e) => e.trip.id === 'd')?.gap).toBeUndefined();
  });

  it('has no gap after an open trip', () => {
    const open = trip('e', '2026-10-21T14:00:00Z', 130);
    expect(logDays([NEXT_DAY, open, NOON])[0].entries[0].gap).toBeUndefined();
  });
});
