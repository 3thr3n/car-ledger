import { describe, expect, it } from 'vitest';
import { EnergyType } from '@/generated';
import { hasBattery, hasTank } from './energy';

describe('energy', () => {
  it('has a tank unless the car is electric', () => {
    expect(hasTank(EnergyType.DIESEL)).toBe(true);
    expect(hasTank(EnergyType.PLUG_IN_HYBRID)).toBe(true);
    expect(hasTank(EnergyType.ELECTRIC)).toBe(false);
  });

  it('has a battery when the car is charged', () => {
    expect(hasBattery(EnergyType.ELECTRIC)).toBe(true);
    expect(hasBattery(EnergyType.PLUG_IN_HYBRID)).toBe(true);
    expect(hasBattery(EnergyType.PETROL)).toBe(false);
  });
});
