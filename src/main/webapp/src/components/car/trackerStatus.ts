import type { TrackerStatus } from '@/generated';

/** a key of tracker.status.warnings */
export type TrackerWarning = 'storageError' | 'config' | 'firmware';

/**
 * What the owner should look at: storage that can't be written, invalid
 * settings, or another firmware than the one offered for flashing
 *
 * @param offered the version of the firmware on the server, if any
 */
export function trackerWarnings(
  status: TrackerStatus,
  offered?: string,
): TrackerWarning[] {
  const warnings: TrackerWarning[] = [];
  if (status.storageError) warnings.push('storageError');
  if (status.configOk === false) warnings.push('config');
  if (offered && status.firmware && status.firmware !== offered) {
    warnings.push('firmware');
  }
  return warnings;
}

/** 0 to 100, undefined without both values */
export function storagePercent(status: TrackerStatus): number | undefined {
  const { storageUsed: used, storageTotal: total } = status;
  if (used === undefined || !total) return undefined;
  return Math.min(100, Math.round((100 * used) / total));
}
