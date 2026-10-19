import { Trip, TripCurrent, TripType } from '@/generated';
import { QueuedRequest } from '@/offline/queue';

/** a trip of the server, or one started on the device and not sent yet */
export type TripView = Trip & {
  /** only on the device: its start is in the offline queue */
  pending?: boolean;
};

/** what the start and the finish dialog need, the server and the queue in one */
export interface TripState {
  open?: TripView;
  /** lowest odometer of the next start in km */
  minimumOdometer: number;
  /** a hint for the start in km, below the minimum it is the minimum */
  suggestedOdometer: number;
}

export const startUrl = (carId: number, id: string) =>
  `/api/trip/${carId}/${id}`;
export const finishUrl = (carId: number, id: string) =>
  `${startUrl(carId, id)}/finish`;

/** the id of the queued finish of a trip: finished again, it is replaced */
export const finishEntryId = (tripId: string) => `${tripId}:finish`;

const odometers = (entry: QueuedRequest) =>
  [entry.body.startOdometer, entry.body.endOdometer].filter(
    (value): value is number => typeof value === 'number',
  );

/** a start of the queue as trip, with the details of its body */
export function fromQueued(entry: QueuedRequest): TripView {
  const body = entry.body;
  return {
    id: entry.tripId!,
    carId: entry.carId,
    type: body.type as TripType,
    startOdometer: body.startOdometer as number,
    startedAt: body.startedAt as string,
    destination: body.destination as string | undefined,
    purpose: body.purpose as string | undefined,
    businessPartner: body.businessPartner as string | undefined,
    restored: false,
    createdAt: entry.createdAt,
    pending: true,
  };
}

/**
 * The state of the server with what the queue of the device adds: a trip
 * started offline is open, a trip finished offline is not, and the odometers
 * of the queue raise the minimum of the next start.
 *
 * @param queued the trip requests of the car, in the order they were made
 */
export function tripState(
  current: TripCurrent | undefined,
  queued: QueuedRequest[],
): TripState {
  const waiting = queued.filter((entry) => entry.status === 'waiting');
  const finished = new Set(
    waiting
      .filter((entry) => entry.tripAction === 'finish')
      .map((entry) => entry.tripId),
  );
  const start = waiting
    .filter((entry) => entry.tripAction === 'start')
    .findLast((entry) => !finished.has(entry.tripId));

  let open: TripView | undefined;
  if (start) {
    open = fromQueued(start);
  } else if (current?.open && !finished.has(current.open.id)) {
    open = current.open;
  }

  const minimumOdometer = Math.max(
    current?.minimumOdometer ?? 0,
    ...waiting.flatMap(odometers),
  );
  return {
    open,
    minimumOdometer,
    suggestedOdometer: Math.max(
      minimumOdometer,
      current?.estimatedOdometer ?? 0,
    ),
  };
}
