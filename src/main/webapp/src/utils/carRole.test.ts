import { describe, expect, it } from 'vitest';
import { Car, CarRole } from '@/generated';
import { isShared, ownCars } from './carRole';

const car = (id: number, role: CarRole) => ({ id, role }) as Car;

describe('ownCars', () => {
  it('leaves out the shared cars', () => {
    const cars = [
      car(1, CarRole.OWNER),
      car(2, CarRole.EDITOR),
      car(3, CarRole.DRIVER),
    ];
    expect(ownCars(cars).map((c) => c.id)).toEqual([1]);
    expect(ownCars(undefined)).toEqual([]);
    expect(isShared(cars[0])).toBe(false);
    expect(isShared(cars[2])).toBe(true);
  });
});
