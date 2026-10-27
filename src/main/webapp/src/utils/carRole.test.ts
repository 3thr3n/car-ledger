import { describe, expect, it } from 'vitest';
import { Car, CarRole } from '@/generated';
import { can, isShared, ownCars } from './carRole';

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

describe('can', () => {
  it('allows each role its actions', () => {
    const actions = ['READ', 'WRITE', 'DRIVE', 'MANAGE'] as const;
    const allowed = (role: CarRole) =>
      actions.filter((action) => can(car(1, role), action));

    expect(allowed(CarRole.OWNER)).toEqual(actions);
    expect(allowed(CarRole.EDITOR)).toEqual(['READ', 'WRITE', 'DRIVE']);
    expect(allowed(CarRole.VIEWER)).toEqual(['READ']);
    expect(allowed(CarRole.DRIVER)).toEqual(['DRIVE']);
  });

  it('allows nothing while the car loads', () => {
    expect(can(undefined, 'READ')).toBe(false);
  });
});
