import { describe, expect, it } from 'vitest';
import { byDay, deleteRange, driveAt, interval, minutes } from './drives';

const drive = (start: string, end: string) => ({
  start,
  end,
  distance: 1,
});

/** newest first, at noon so the local day is the same in every time zone */
const EVENING = drive('2026-10-21T12:30:00Z', '2026-10-21T12:52:40Z');
const MORNING = drive('2026-10-21T11:00:00Z', '2026-10-21T11:00:20Z');
const DAY_BEFORE = drive('2026-10-20T12:00:00Z', '2026-10-20T13:15:00Z');

describe('drives', () => {
  it('groups by the day of the start, in their order', () => {
    expect(byDay([EVENING, MORNING, DAY_BEFORE])).toEqual([
      { day: '2026-10-21', drives: [EVENING, MORNING] },
      { day: '2026-10-20', drives: [DAY_BEFORE] },
    ]);
    expect(byDay([])).toEqual([]);
  });

  it('counts whole minutes, at least one', () => {
    expect(minutes(EVENING)).toBe(23);
    expect(minutes(MORNING)).toBe(1);
    expect(minutes(DAY_BEFORE)).toBe(75);
  });

  it('finds the drive of a time, both ends included', () => {
    const drives = [EVENING, MORNING, DAY_BEFORE];

    expect(driveAt(drives, '2026-10-21T12:30:00Z')).toBe(EVENING);
    expect(driveAt(drives, '2026-10-21T12:52:40Z')).toBe(EVENING);
    expect(driveAt(drives, '2026-10-21T11:00:10.500Z')).toBe(MORNING);
    expect(driveAt(drives, '2026-10-21T12:00:00Z')).toBeUndefined();
  });

  it('writes the interval', () => {
    expect(interval(MORNING)).toBe('2026-10-21T11:00:00Z/2026-10-21T11:00:20Z');
  });

  it('deletes a drive with the stop after it', () => {
    const drives = [EVENING, MORNING, DAY_BEFORE];
    const end = '2026-10-22T00:00:00Z';

    expect(deleteRange(drives, DAY_BEFORE, end)).toEqual({
      from: DAY_BEFORE.start,
      to: MORNING.start,
    });
    expect(deleteRange(drives, EVENING, end)).toEqual({
      from: EVENING.start,
      to: end,
    });
  });
});
