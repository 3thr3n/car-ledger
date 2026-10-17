import { EnergyType, MonthStats } from '@/generated';
import { num } from '@/utils/format';
import { hasBattery, hasTank } from '@/utils/energy';

export interface MonthlyParts {
  /** fuel is a part of the costs */
  fuelCosts: boolean;
  /** charging is a part of the costs */
  chargingCosts: boolean;
  /** the price and consumption of the refuels can be selected */
  fuelView: boolean;
  /** the price and consumption of the charges can be selected */
  chargingView: boolean;
}

/**
 * Which parts of the monthly chart are shown: the ones with data, and the ones
 * the shown cars can have, e.g. no fuel for an electric car without old
 * refuels.
 *
 * @param energyTypes
 *            of the shown cars, unknown while they are loaded
 */
export function monthlyParts(
  months: MonthStats[],
  energyTypes?: EnergyType[],
): MonthlyParts {
  const hasFuelCosts = months.some((month) => (num(month.fuel) ?? 0) > 0);
  const hasChargingCosts = months.some(
    (month) => (num(month.charging) ?? 0) > 0,
  );
  const hasFuel = months.some((month) => num(month.pricePerUnit) !== undefined);
  const hasCharging = months.some(
    (month) => num(month.chargingPricePerUnit) !== undefined,
  );
  const canFuel = energyTypes?.some(hasTank);
  const canCharge = energyTypes?.some(hasBattery);
  return {
    fuelCosts: hasFuelCosts || (canFuel ?? true),
    chargingCosts: hasChargingCosts || (canCharge ?? false),
    // without known cars, the fuel view stays when there are no charges
    fuelView: hasFuel || (canFuel ?? !hasCharging),
    chargingView: hasCharging || (canCharge ?? false),
  };
}
