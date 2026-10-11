import { describe, expect, it } from 'vitest';
import { billTypes, isBillType } from './billTypes';
import { BillType } from '@/generated';

describe('billTypes', () => {
  it('only allows importing types the CSV import supports', () => {
    expect(billTypes[BillType.RECURRING].importable).toBe(false);
    expect(billTypes[BillType.FUEL].importable).toBe(true);
  });

  it('recognizes bill types', () => {
    expect(isBillType('FUEL')).toBe(true);
    expect(isBillType('fuel')).toBe(false);
    expect(isBillType(undefined)).toBe(false);
  });
});
