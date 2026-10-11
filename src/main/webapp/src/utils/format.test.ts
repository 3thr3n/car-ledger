import { describe, expect, it } from 'vitest';
import { hasValue, num, toApiDate } from './format';
import dayjs from 'dayjs';

describe('num', () => {
  it('converts numbers and numeric strings', () => {
    expect(num(12.5)).toBe(12.5);
    expect(num('12.30')).toBe(12.3);
  });

  it('is undefined for missing or invalid values', () => {
    expect(num(undefined)).toBeUndefined();
    expect(num(null)).toBeUndefined();
    expect(num('')).toBeUndefined();
    expect(num('abc')).toBeUndefined();
  });
});

describe('hasValue', () => {
  it('treats zero as no value', () => {
    expect(hasValue(0)).toBe(false);
    expect(hasValue('0.00')).toBe(false);
    expect(hasValue(0.01)).toBe(true);
  });
});

describe('toApiDate', () => {
  it('formats dates like the API expects them', () => {
    expect(toApiDate(dayjs('2026-03-04T15:00:00'))).toBe('2026-03-04');
    expect(toApiDate(null)).toBeUndefined();
  });
});
