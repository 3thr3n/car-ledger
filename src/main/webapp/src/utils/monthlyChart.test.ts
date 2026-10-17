import { describe, expect, it } from 'vitest';
import { EnergyType, MonthStats } from '@/generated';
import { monthlyParts } from './monthlyChart';

const month = (values: Partial<MonthStats> = {}) =>
  ({ month: '2026-09', ...values }) as MonthStats;

describe('monthlyParts', () => {
  it('shows no fuel for an electric car', () => {
    const parts = monthlyParts(
      [month({ charging: 40, chargingPricePerUnit: 35, total: 40 })],
      [EnergyType.ELECTRIC],
    );
    expect(parts).toEqual({
      fuelCosts: false,
      chargingCosts: true,
      fuelView: false,
      chargingView: true,
    });
  });

  it('shows no fuel for an electric car without any entries', () => {
    const parts = monthlyParts([month()], [EnergyType.ELECTRIC]);
    expect(parts.fuelCosts).toBe(false);
    expect(parts.fuelView).toBe(false);
    expect(parts.chargingView).toBe(true);
  });

  it('shows old refuels of an electric car', () => {
    const parts = monthlyParts(
      [month({ fuel: 60, pricePerUnit: 180 })],
      [EnergyType.ELECTRIC],
    );
    expect(parts.fuelCosts).toBe(true);
    expect(parts.fuelView).toBe(true);
  });

  it('shows no charging for a petrol car', () => {
    const parts = monthlyParts([month()], [EnergyType.PETROL]);
    expect(parts).toEqual({
      fuelCosts: true,
      chargingCosts: false,
      fuelView: true,
      chargingView: false,
    });
  });

  it('shows both for a plug-in hybrid without entries', () => {
    const parts = monthlyParts([month()], [EnergyType.PLUG_IN_HYBRID]);
    expect(parts.fuelView).toBe(true);
    expect(parts.chargingView).toBe(true);
  });

  it('shows what one of all cars can have', () => {
    const parts = monthlyParts(
      [month()],
      [EnergyType.ELECTRIC, EnergyType.DIESEL],
    );
    expect(parts.fuelView).toBe(true);
    expect(parts.chargingView).toBe(true);
  });

  it('uses only the data while the cars are unknown', () => {
    expect(monthlyParts([month()])).toEqual({
      fuelCosts: true,
      chargingCosts: false,
      fuelView: true,
      chargingView: false,
    });
    const charged = monthlyParts([
      month({ charging: 40, chargingPricePerUnit: 35 }),
    ]);
    expect(charged.fuelView).toBe(false);
    expect(charged.chargingView).toBe(true);
  });
});
