import { UnitSystem } from '@/generated';

/** exact by definition */
export const KM_PER_MILE = 1.609344;
export const LITERS_PER_US_GALLON = 3.785411784;
export const LITERS_PER_IMPERIAL_GALLON = 4.54609;

/** i18n context of the texts that name the distance or the volume */
export type DistanceContext = 'miles' | undefined;
export type VolumeContext = 'gallons' | undefined;

/**
 * The units of a unit system. The API and the database always use km, liters
 * and cent per liter; these convert to the units shown and entered and back.
 */
export interface Units {
  system: UnitSystem;
  distance: {
    label: 'km' | 'mi';
    context: DistanceContext;
    fromKm: (km: number) => number;
    toKm: (value: number) => number;
  };
  volume: {
    label: 'l' | 'gal';
    context: VolumeContext;
    fromLiters: (liters: number) => number;
    toLiters: (value: number) => number;
  };
  fuelPrice: {
    /** the price per gallon is in the currency, the one per liter in cent */
    inCurrency: boolean;
    /** digits as shown at the pump */
    digits: number;
    fromCentsPerLiter: (cents: number) => number;
    toCentsPerLiter: (value: number) => number;
  };
  consumption: {
    label: 'l/100km' | 'mpg';
    /** mpg is the distance per volume, more is better */
    digits: number;
    fromLitersPer100Km: (value: number) => number;
    toLitersPer100Km: (value: number) => number;
  };
  chargingConsumption: {
    label: 'kWh/100km' | 'kWh/100mi';
    fromKwhPer100Km: (value: number) => number;
  };
  costPerDistance: {
    label: 'ct/km' | 'ct/mi';
    fromCentsPerKm: (value: number) => number;
  };
}

const identity = (value: number) => value;

/**
 * mpg and l/100km are inverse: mpg = 100 * liters per gallon / km per mile /
 * (l/100km), the same formula both ways
 */
const mpg = (litersPerGallon: number) => {
  const factor = (100 * litersPerGallon) / KM_PER_MILE;
  return (value: number) => (value > 0 ? factor / value : 0);
};

const metricDistance: Units['distance'] = {
  label: 'km',
  context: undefined,
  fromKm: identity,
  toKm: identity,
};

const miles: Units['distance'] = {
  label: 'mi',
  context: 'miles',
  fromKm: (km) => km / KM_PER_MILE,
  toKm: (value) => value * KM_PER_MILE,
};

const liters: Units['volume'] = {
  label: 'l',
  context: undefined,
  fromLiters: identity,
  toLiters: identity,
};

const centsPerLiter: Units['fuelPrice'] = {
  inCurrency: false,
  digits: 1,
  fromCentsPerLiter: identity,
  toCentsPerLiter: identity,
};

const perMile = {
  chargingConsumption: {
    label: 'kWh/100mi',
    fromKwhPer100Km: (value: number) => value * KM_PER_MILE,
  },
  costPerDistance: {
    label: 'ct/mi',
    fromCentsPerKm: (value: number) => value * KM_PER_MILE,
  },
} as const;

const systems: Record<UnitSystem, Units> = {
  [UnitSystem.METRIC]: {
    system: UnitSystem.METRIC,
    distance: metricDistance,
    volume: liters,
    fuelPrice: centsPerLiter,
    consumption: {
      label: 'l/100km',
      digits: 2,
      fromLitersPer100Km: identity,
      toLitersPer100Km: identity,
    },
    chargingConsumption: { label: 'kWh/100km', fromKwhPer100Km: identity },
    costPerDistance: { label: 'ct/km', fromCentsPerKm: identity },
  },
  [UnitSystem.US]: {
    system: UnitSystem.US,
    distance: miles,
    volume: {
      label: 'gal',
      context: 'gallons',
      fromLiters: (value) => value / LITERS_PER_US_GALLON,
      toLiters: (value) => value * LITERS_PER_US_GALLON,
    },
    fuelPrice: {
      inCurrency: true,
      digits: 3,
      fromCentsPerLiter: (value) => (value * LITERS_PER_US_GALLON) / 100,
      toCentsPerLiter: (value) => (value * 100) / LITERS_PER_US_GALLON,
    },
    consumption: {
      label: 'mpg',
      digits: 1,
      fromLitersPer100Km: mpg(LITERS_PER_US_GALLON),
      toLitersPer100Km: mpg(LITERS_PER_US_GALLON),
    },
    ...perMile,
  },
  // fuel is sold in liters, the consumption is in imperial mpg
  [UnitSystem.UK]: {
    system: UnitSystem.UK,
    distance: miles,
    volume: liters,
    fuelPrice: centsPerLiter,
    consumption: {
      label: 'mpg',
      digits: 1,
      fromLitersPer100Km: mpg(LITERS_PER_IMPERIAL_GALLON),
      toLitersPer100Km: mpg(LITERS_PER_IMPERIAL_GALLON),
    },
    ...perMile,
  },
};

export function unitsOf(system?: UnitSystem): Units {
  return systems[system ?? UnitSystem.METRIC];
}

/**
 * A value of the API for a field: in the units shown, rounded to the digits
 * of the field, so a converted value does not show endless digits
 */
export function toField(
  value: number | null | undefined,
  conversion: (value: number) => number,
  digits: number,
): number | undefined {
  const converted = convert(value, conversion);
  if (converted === undefined) return undefined;
  const factor = 10 ** digits;
  return Math.round(converted * factor) / factor;
}

/** an odometer or interval of the API (whole km) for a field */
export function odometerToField(units: Units, km: number | null | undefined) {
  return toField(km, units.distance.fromKm, 0);
}

/** an odometer or interval of a field for the API, in whole km */
export function odometerToApi(units: Units, value: number | undefined) {
  return convert(value, (distance) =>
    Math.round(units.distance.toKm(distance)),
  );
}

/** a value of the API in the units shown, undefined stays undefined */
export function convert(
  value: number | null | undefined,
  conversion: (value: number) => number,
): number | undefined {
  return value === null || value === undefined ? undefined : conversion(value);
}
