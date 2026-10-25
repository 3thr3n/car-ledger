import { describe, expect, it } from 'vitest';
import { fileSize, hasValue, num, toApiDate } from './format';
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

describe('fileSize', () => {
  it('uses the largest unit below 1024', () => {
    expect(fileSize('en-US', 500)).toBe('500 byte');
    expect(fileSize('en-US', 300 * 1024)).toBe('300 kB');
    expect(fileSize('en-US', 1.25 * 1024 * 1024 * 1024)).toBe('1.3 GB');
    expect(fileSize('de-DE', 2.5 * 1024 * 1024)).toBe('2,5 MB');
  });
});
