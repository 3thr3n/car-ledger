import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BillType, Car, EnergyType } from '@/generated';
import { rememberCar, shortcutCar, shortcutType } from '@/utils/lastCar';

const car = (id: number, energyType = EnergyType.PETROL) =>
  ({ id, name: `Car ${id}`, energyType }) as Car;

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

  it('charges electric cars and refuels the others', () => {
    expect(shortcutType(car(1, EnergyType.ELECTRIC))).toBe(BillType.CHARGING);
    expect(shortcutType(car(1, EnergyType.PLUG_IN_HYBRID))).toBe(BillType.FUEL);
    expect(shortcutType(car(1, EnergyType.DIESEL))).toBe(BillType.FUEL);
  });
});
