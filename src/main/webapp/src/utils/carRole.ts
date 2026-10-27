import { Car, CarRole } from '@/generated';

/** the cars of the user as owner: the car limit and the backup count only them */
export const ownCars = (cars: Car[] | undefined) =>
  (cars ?? []).filter((car) => car.role === CarRole.OWNER);

/** a car another account shared with the user */
export const isShared = (car: Car) => car.role !== CarRole.OWNER;

/**
 * What the user may do with a car, the same as CarAccess of the backend; the
 * server refuses the rest anyway
 * - READ: see the car and all its entries
 * - WRITE: add, change and delete entries
 * - DRIVE: trips and refuels (a driver only the own ones)
 * - MANAGE: change and delete the car, its trackers and its access
 */
export type CarAction = 'READ' | 'WRITE' | 'DRIVE' | 'MANAGE';

const ROLES: Record<CarAction, readonly CarRole[]> = {
  READ: [CarRole.OWNER, CarRole.EDITOR, CarRole.VIEWER],
  WRITE: [CarRole.OWNER, CarRole.EDITOR],
  DRIVE: [CarRole.OWNER, CarRole.EDITOR, CarRole.DRIVER],
  MANAGE: [CarRole.OWNER],
};

/** @return whether the role of the user allows the action, not while loading */
export const can = (car: Pick<Car, 'role'> | undefined, action: CarAction) =>
  !!car && ROLES[action].includes(car.role);
