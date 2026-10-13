import { describe, expect, it } from 'vitest';
import { normalizeVin, vinError } from './vin';

describe('vin', () => {
  it('removes spaces and dashes', () => {
    expect(normalizeVin(' wvw zzz1kz-6w000001 ')).toBe('WVWZZZ1KZ6W000001');
  });

  it('accepts an empty VIN', () => {
    expect(vinError('  ', 2020)).toBeUndefined();
  });

  it('needs 17 characters without I, O and Q since 1981', () => {
    expect(vinError('WVWZZZ1KZ6W000001', 2020)).toBeUndefined();
    expect(vinError('WVWZZZ1KZ6W00000I', 2020)).toBe('vin');
    expect(vinError('1180012345', 1981)).toBe('vin');
  });

  it('accepts shorter numbers of older cars', () => {
    expect(vinError('1180012345', 1975)).toBeUndefined();
    expect(vinError('118001234567890123', 1975)).toBe('vinOld');
  });
});
