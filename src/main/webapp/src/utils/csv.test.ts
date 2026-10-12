import { describe, expect, it } from 'vitest';
import { guessMapping, restoreMapping, toSavedMapping } from './csv';

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

describe('saved mapping', () => {
  const fields = ['date', 'unit', 'total'];

  it('stores column names, null for fields that are not imported', () => {
    expect(
      toSavedMapping(fields, ['Datum', 'Liter', 'Summe'], { date: 0, unit: 1 }),
    ).toEqual({ date: 'Datum', unit: 'Liter', total: null });
  });

  it('finds the saved columns in another column order', () => {
    const saved = { date: 'Datum', unit: 'Liter', total: null };

    expect(
      restoreMapping(fields, ['Liter', 'Notiz', 'Datum', 'Total'], saved),
    ).toEqual({ mapping: { date: 2, unit: 0 }, restored: true });
  });

  it('guesses fields the saved mapping does not know', () => {
    const saved = { date: 'Datum', unit: 'Liter' };

    expect(restoreMapping(fields, ['Datum', 'Liter', 'Total'], saved)).toEqual({
      mapping: { date: 0, unit: 1, total: 2 },
      restored: true,
    });
  });

  it('guesses everything for a file of another export', () => {
    const saved = { date: 'Datum', unit: 'Liter', total: null };

    expect(restoreMapping(fields, ['date', 'total'], saved)).toEqual({
      mapping: { date: 0, total: 1 },
      restored: false,
    });
  });
});
