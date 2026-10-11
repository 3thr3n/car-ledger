import { describe, expect, it } from 'vitest';
import { derive } from './fuel';

describe('derive', () => {
  it('calculates the total from liters and price per liter', () => {
    expect(derive({ unit: 40, pricePerUnit: 180 }).total).toBeCloseTo(72);
  });

  it('calculates the liters from total and price per liter', () => {
    expect(derive({ total: 72, pricePerUnit: 180 }).unit).toBeCloseTo(40);
  });

  it('calculates the price per liter from total and liters', () => {
    expect(derive({ total: 72, unit: 40 }).pricePerUnit).toBeCloseTo(180);
  });

  it('calculates nothing with all or less than two values', () => {
    expect(derive({ total: 72, unit: 40, pricePerUnit: 180 })).toEqual({});
    expect(derive({ total: 72 })).toEqual({});
    // zero counts as not set
    expect(derive({ total: 72, unit: 0 })).toEqual({});
  });
});
