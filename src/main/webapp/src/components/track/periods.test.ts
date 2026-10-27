import dayjs from 'dayjs';
import { describe, expect, it } from 'vitest';
import { rangeOf, shortcutOf } from './periods';

const today = dayjs('2026-10-28');

describe('shortcuts for the period of the map', () => {
  it('ends every period today', () => {
    expect(rangeOf('today', today)).toEqual({
      from: '2026-10-28',
      to: '2026-10-28',
    });
    expect(rangeOf('last3', today)).toEqual({
      from: '2026-10-26',
      to: '2026-10-28',
    });
    expect(rangeOf('last7', today)).toEqual({
      from: '2026-10-22',
      to: '2026-10-28',
    });
    expect(rangeOf('month', today)).toEqual({
      from: '2026-10-01',
      to: '2026-10-28',
    });
  });

  it('finds the shortcut of a period', () => {
    expect(shortcutOf({ from: '2026-10-22', to: '2026-10-28' }, today)).toBe(
      'last7',
    );
    // moved with the arrows or chosen by hand
    expect(shortcutOf({ from: '2026-10-15', to: '2026-10-21' }, today)).toBe(
      undefined,
    );
    expect(shortcutOf({ from: '2026-10-22' }, today)).toBe(undefined);
  });

  it('prefers today on the first day of the month', () => {
    const first = dayjs('2026-11-01');
    expect(shortcutOf(rangeOf('month', first), first)).toBe('today');
  });
});
