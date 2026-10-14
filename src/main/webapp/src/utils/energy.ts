import { EnergyType } from '@/generated';

/** the car is refueled, so it has a tank (same as the backend) */
export const hasTank = (type?: EnergyType) => type !== EnergyType.ELECTRIC;

/** the car is charged, so it has a battery (same as the backend) */
export const hasBattery = (type?: EnergyType) =>
  type === EnergyType.ELECTRIC || type === EnergyType.PLUG_IN_HYBRID;

/** liters, the same limit as the backend */
export const MAX_TANK_SIZE = 1000;
/** kWh, the same limit as the backend */
export const MAX_BATTERY_CAPACITY = 500;
