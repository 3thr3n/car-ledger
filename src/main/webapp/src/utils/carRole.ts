import { Car, CarRole } from '@/generated';

/** the cars of the user as owner: the car limit and the backup count only them */
export const ownCars = (cars: Car[] | undefined) =>
  (cars ?? []).filter((car) => car.role === CarRole.OWNER);

/** a car another account shared with the user */
export const isShared = (car: Car) => car.role !== CarRole.OWNER;
