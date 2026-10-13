/** since 1981 the VIN has 17 characters without I, O and Q (ISO 3779) */
const VIN = /^[A-HJ-NPR-Z0-9]{17}$/;
/** older cars have shorter numbers of the manufacturer */
const OLD_VIN = /^[A-Z0-9]{1,17}$/;
const VIN_SINCE = 1981;

/** as saved by the API: upper case, without spaces and dashes */
export const normalizeVin = (vin: string) =>
  vin.replace(/[\s-]/g, '').toUpperCase();

/**
 * @return key of the error, undefined for an empty or valid VIN
 */
export function vinError(
  vin: string,
  year: number | undefined,
): 'vin' | 'vinOld' | undefined {
  const value = normalizeVin(vin);
  if (!value) return undefined;
  if (year === undefined || year >= VIN_SINCE) {
    return VIN.test(value) ? undefined : 'vin';
  }
  return OLD_VIN.test(value) ? undefined : 'vinOld';
}
