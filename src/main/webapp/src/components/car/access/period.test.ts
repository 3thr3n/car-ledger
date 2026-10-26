import dayjs from 'dayjs';
import { describe, expect, it } from 'vitest';
import { firstDay, fromDay, hasEnded, lastDay, untilDay } from './period';

describe('period of an access', () => {
  const day = dayjs('2026-11-02T15:30:00');

  it('starts with the first day and ends after the last one', () => {
    expect(dayjs(fromDay(day)).format()).toBe(
      dayjs('2026-11-02T00:00:00').format(),
    );
    expect(dayjs(untilDay(day)).format()).toBe(
      dayjs('2026-11-03T00:00:00').format(),
    );
  });

  it('shows the days it was saved with', () => {
    const period = { validFrom: fromDay(day), validUntil: untilDay(day) };

    expect(firstDay(period)?.format('YYYY-MM-DD')).toBe('2026-11-02');
    expect(lastDay(period)?.format('YYYY-MM-DD')).toBe('2026-11-02');
  });

  it('has no start and no end without days', () => {
    expect(fromDay(null)).toBeUndefined();
    expect(untilDay(null)).toBeUndefined();
    expect(firstDay({})).toBeNull();
    expect(lastDay({ validUntil: null })).toBeNull();
  });

  it('has ended once its end passed', () => {
    const period = { validUntil: untilDay(day) };

    expect(hasEnded(period, dayjs('2026-11-02T23:59:00'))).toBe(false);
    expect(hasEnded(period, dayjs('2026-11-03T00:00:00'))).toBe(true);
    expect(hasEnded({})).toBe(false);
  });
});
