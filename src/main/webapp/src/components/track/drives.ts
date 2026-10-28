import dayjs from 'dayjs';
import type { TrackDrive } from '@/generated';

/** the drives of a day, newest first like the list of the API */
export interface DriveDay {
  /** yyyy-MM-dd in the time zone of the device */
  day: string;
  drives: TrackDrive[];
}

/** groups the drives by the local day of their start, in their order */
export function byDay(drives: TrackDrive[]): DriveDay[] {
  const days: DriveDay[] = [];
  for (const drive of drives) {
    const day = dayjs(drive.start).format('YYYY-MM-DD');
    const last = days.at(-1);
    if (last?.day === day) last.drives.push(drive);
    else days.push({ day, drives: [drive] });
  }
  return days;
}

/** whole minutes, at least one */
export const minutes = (drive: TrackDrive) =>
  Math.max(1, Math.round(dayjs(drive.end).diff(drive.start, 'second') / 60));

/** the drive at the time, e.g. of a point of the track */
export const driveAt = (drives: TrackDrive[], ts: string) => {
  const time = dayjs(ts);
  return drives.find(
    (drive) => !time.isBefore(drive.start) && !time.isAfter(drive.end),
  );
};

/** as the API takes it: start/end */
export const interval = (drive: TrackDrive) => `${drive.start}/${drive.end}`;

/**
 * The range to delete a drive: its points and the stop after it, up to the
 * start of the next drive (the range of the API excludes its end), or the end
 * of the period. Only the drive would leave the stops before and after it,
 * which follow each other within minutes after a short drive and would be
 * joined by a straight line, a drive again.
 *
 * @param drives newest first, as the API lists them
 */
export function deleteRange(
  drives: TrackDrive[],
  drive: TrackDrive,
  periodEnd: string,
) {
  const next = drives[drives.indexOf(drive) - 1];
  return { from: drive.start, to: next?.start ?? periodEnd };
}
