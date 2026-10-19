import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BillType, Car, EnergyType } from '@/generated';
import {
  rememberCar,
  shortcutCar,
  shortcutTripCar,
  shortcutType,
} from '@/utils/lastCar';

const car = (id: number, energyType = EnergyType.PETROL, tripLog = false) =>
  ({ id, name: `Car ${id}`, energyType, tripLog }) as Car;
const logged = (id: number) => car(id, EnergyType.PETROL, true);

describe('shortcut "Add refuel"', () => {
  // the tests run in Node, without the storage of the browser
  beforeEach(() => {
    const items = new Map<string, string>();
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => items.get(key) ?? null,
      setItem: (key: string, value: string) => items.set(key, value),
    });
  });

  it('takes the last opened car', () => {
    rememberCar(2);
    expect(shortcutCar([car(1), car(2)])?.id).toBe(2);
  });

  it('takes the first car without one or when it is gone', () => {
    expect(shortcutCar([car(1), car(2)])?.id).toBe(1);
    rememberCar(3);
    expect(shortcutCar([car(1), car(2)])?.id).toBe(1);
  });

  it('has no car without cars', () => {
    expect(shortcutCar([])).toBeUndefined();
  });

  it('takes the last opened car with the trip log for the trips', () => {
    rememberCar(3);
    expect(shortcutTripCar([logged(1), car(2), logged(3)])?.id).toBe(3);
  });

  it('takes the first car with the trip log for the trips', () => {
    rememberCar(2);
    expect(shortcutTripCar([car(1), car(2), logged(3)])?.id).toBe(3);
    expect(shortcutTripCar([car(1), car(2)])).toBeUndefined();
  });

  it('charges electric cars and refuels the others', () => {
    expect(shortcutType(car(1, EnergyType.ELECTRIC))).toBe(BillType.CHARGING);
    expect(shortcutType(car(1, EnergyType.PLUG_IN_HYBRID))).toBe(BillType.FUEL);
    expect(shortcutType(car(1, EnergyType.DIESEL))).toBe(BillType.FUEL);
  });
});
