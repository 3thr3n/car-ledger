import { describe, expect, it } from 'vitest';
import { guessMapping } from './csv';

describe('guessMapping', () => {
  it('maps fields to the columns with matching names', () => {
    const mapping = guessMapping(
      ['date', 'unit', 'pricePerUnit', 'total'],
      ['Date', 'Price per unit', 'Total €', 'Unit (l)'],
    );

    expect(mapping).toEqual({ date: 0, pricePerUnit: 1, total: 2, unit: 3 });
  });

  it('leaves fields without a matching column out', () => {
    expect(guessMapping(['workshop'], ['date', 'total'])).toEqual({});
  });
});
