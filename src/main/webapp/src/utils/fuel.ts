export interface FuelAmounts {
  total?: number;
  unit?: number;
  pricePerUnit?: number;
}

export const isSet = (value?: number) => value !== undefined && value > 0;

/**
 * Same calculation as the backend: two of total (€), unit (l) and price per
 * unit (ct/l) define the third
 */
export function derive({
  total,
  unit,
  pricePerUnit,
}: FuelAmounts): FuelAmounts {
  if (isSet(unit) && isSet(pricePerUnit) && !isSet(total)) {
    return { total: (unit! * pricePerUnit!) / 100 };
  }
  if (isSet(total) && isSet(pricePerUnit) && !isSet(unit)) {
    return { unit: total! / (pricePerUnit! / 100) };
  }
  if (isSet(total) && isSet(unit) && !isSet(pricePerUnit)) {
    return { pricePerUnit: (total! * 100) / unit! };
  }
  return {};
}
