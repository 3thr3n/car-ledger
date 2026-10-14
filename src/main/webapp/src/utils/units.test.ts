import { describe, expect, it } from 'vitest';
import { UnitSystem } from '@/generated';
import { convert, unitsOf } from './units';

describe('units', () => {
  it('keeps the metric values', () => {
    const units = unitsOf(UnitSystem.METRIC);
    expect(units.distance.fromKm(100)).toBe(100);
    expect(units.consumption.fromLitersPer100Km(6.4)).toBe(6.4);
    expect(units.fuelPrice.fromCentsPerLiter(172.9)).toBe(172.9);
  });

  it('converts to miles, US gallons and mpg', () => {
    const units = unitsOf(UnitSystem.US);
    expect(units.distance.fromKm(160.9344)).toBeCloseTo(100);
    expect(units.volume.fromLiters(37.854118)).toBeCloseTo(10);
    // 2.35 l/100km are about 100 mpg (US)
    expect(units.consumption.fromLitersPer100Km(2.352146)).toBeCloseTo(100);
    // the price per gallon is in the currency
    expect(units.fuelPrice.fromCentsPerLiter(91.38)).toBeCloseTo(3.459, 3);
    expect(units.chargingConsumption.fromKwhPer100Km(18)).toBeCloseTo(28.97);
  });

  it('uses liters and imperial mpg in the UK', () => {
    const units = unitsOf(UnitSystem.UK);
    expect(units.volume.label).toBe('l');
    expect(units.distance.label).toBe('mi');
    // 6 l/100km are about 47.1 mpg (imperial)
    expect(units.consumption.fromLitersPer100Km(6)).toBeCloseTo(47.08, 2);
  });

  it('converts back what it converted', () => {
    for (const system of Object.values(UnitSystem)) {
      const units = unitsOf(system);
      expect(units.distance.toKm(units.distance.fromKm(512.3))).toBeCloseTo(
        512.3,
      );
      expect(units.volume.toLiters(units.volume.fromLiters(41.02))).toBeCloseTo(
        41.02,
      );
      expect(
        units.consumption.toLitersPer100Km(
          units.consumption.fromLitersPer100Km(5.47),
        ),
      ).toBeCloseTo(5.47);
      expect(
        units.fuelPrice.toCentsPerLiter(
          units.fuelPrice.fromCentsPerLiter(185.9),
        ),
      ).toBeCloseTo(185.9);
    }
  });

  it('keeps missing values and no consumption', () => {
    const units = unitsOf(UnitSystem.US);
    expect(convert(undefined, units.distance.fromKm)).toBeUndefined();
    expect(convert(null, units.distance.fromKm)).toBeUndefined();
    expect(units.consumption.fromLitersPer100Km(0)).toBe(0);
  });
});
