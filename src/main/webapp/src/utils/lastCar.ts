import { BillType, Car } from '@/generated';
import { hasTank } from '@/utils/energy';

const KEY = 'lastCar';

/** remembers the car the user opened last, for the shortcut "Add refuel" */
export function rememberCar(id: number) {
  try {
    localStorage.setItem(KEY, String(id));
  } catch {
    // e.g. storage blocked: the shortcut takes the first car
  }
}

function lastCarId(): number | undefined {
  try {
    const id = Number(localStorage.getItem(KEY));
    return Number.isInteger(id) && id > 0 ? id : undefined;
  } catch {
    return undefined;
  }
}

/**
 * The car of the shortcut: the last opened one while it exists, else the first
 * one; undefined without cars
 */
export function shortcutCar(cars: Car[]): Car | undefined {
  const last = lastCarId();
  return cars.find((car) => car.id === last) ?? cars[0];
}

/**
 * The car of the shortcut "Trip": the last opened one with the trip log, else
 * the first one with it; undefined without such a car
 */
export function shortcutTripCar(cars: Car[]): Car | undefined {
  const last = lastCarId();
  const withLog = cars.filter((car) => car.tripLog);
  return withLog.find((car) => car.id === last) ?? withLog[0];
}

/** refuel for cars with a tank, charging for electric cars */
export const shortcutType = (car: Car) =>
  hasTank(car.energyType) ? BillType.FUEL : BillType.CHARGING;
