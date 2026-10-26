import { describe, expect, it } from 'vitest';
import { billTypes, billTypesFor, isBillType } from './billTypes';
import { BillType, EnergyType } from '@/generated';

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

  it('offers refuels with a tank and charges with a battery', () => {
    const types = (energyType: EnergyType) =>
      billTypesFor(energyType).map((meta) => meta.type);
    expect(types(EnergyType.DIESEL)).toEqual([
      BillType.FUEL,
      BillType.MAINTENANCE,
      BillType.MISCELLANEOUS,
      BillType.RECURRING,
    ]);
    expect(types(EnergyType.ELECTRIC)).not.toContain(BillType.FUEL);
    expect(types(EnergyType.PLUG_IN_HYBRID)).toEqual(
      expect.arrayContaining([BillType.FUEL, BillType.CHARGING]),
    );
  });

  it('offers a driver only refuels and charges', () => {
    const types = (energyType: EnergyType) =>
      billTypesFor(energyType, true).map((meta) => meta.type);
    expect(types(EnergyType.DIESEL)).toEqual([BillType.FUEL]);
    expect(types(EnergyType.PLUG_IN_HYBRID)).toEqual([
      BillType.FUEL,
      BillType.CHARGING,
    ]);
  });
});
