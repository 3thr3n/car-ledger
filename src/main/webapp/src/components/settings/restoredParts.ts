import { BackupSummary } from '@/generated';

/** what a restore names, in this order; the track points are left out */
const PARTS = [
  'cars',
  'bills',
  'recurringBills',
  'reminders',
  'trips',
  'modifications',
  'technicalInspections',
  'places',
  'trackers',
  'files',
  'invitations',
] as const;

export type RestoredPart = (typeof PARTS)[number];

/**
 * @return the parts of a restore with more than zero, e.g. no trips for an
 *         account without the trip log
 */
export function restoredParts(summary: BackupSummary) {
  return PARTS.map((part) => ({ part, count: summary[part] ?? 0 })).filter(
    ({ count }) => count > 0,
  );
}
