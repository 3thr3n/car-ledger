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

/** a drive of a day as a part of its route */
export interface Leg {
  drive: TrackDrive;
  /**
   * the place it starts from is shown: the first drive of the day, or the one
   * before it ended at another place, e.g. the tracker was off on the way
   */
  departure: boolean;
  /** whole minutes parked at its end until the next drive of the day */
  parked?: number;
}

/**
 * The drives of a day as its route, from the morning on: from stop to stop,
 * each stop with the time parked there
 *
 * @param drives of one day, newest first, as the API lists them
 */
export function legsOf(drives: TrackDrive[]): Leg[] {
  const route = [...drives].reverse();
  return route.map((drive, index) => {
    const before = route[index - 1];
    const after = route[index + 1];
    return {
      drive,
      departure: !before || before.to !== drive.from,
      parked: after
        ? Math.max(
            0,
            Math.round(dayjs(after.start).diff(drive.end, 'second') / 60),
          )
        : undefined,
    };
  });
}

/** the minutes as hours and minutes, e.g. for 125: 2 h 5 min */
export const hoursOf = (total: number) => ({
  hours: Math.floor(total / 60),
  minutes: total % 60,
});
