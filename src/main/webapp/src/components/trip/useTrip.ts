import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import {
  finishTrip,
  startTrip,
  TripFinishInput,
  TripStartInput,
} from '@/generated';
import { getCurrentTripOptions } from '@/generated/@tanstack/react-query.gen';
import useMe from '@/hooks/useMe';
import { isOfflineError, offlineQueue } from '@/offline';
import { useQueued, useQueueRequest } from '@/offline/useQueued';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import {
  finishEntryId,
  finishUrl,
  startUrl,
  tripState,
  TripView,
} from './tripState';

/** saved on the server, or kept on the device until there is a connection */
export type TripResult = 'saved' | 'queued';

export type StartDetails = Omit<TripStartInput, 'startedAt' | 'queued'>;
export type FinishDetails = Omit<TripFinishInput, 'endedAt' | 'queued'>;

/** the start and finish requests of the queue for a car */
export function useQueuedTrips(carId: number) {
  const queued = useQueued(carId);
  return useMemo(() => queued.filter((entry) => entry.tripId), [queued]);
}

/**
 * The open trip and the minimum odometer of a car, also without a connection:
 * the last state of the server is kept on the device, the queue adds the trips
 * started or finished offline.
 */
export function useTripState(carId: number) {
  const { data, isLoading } = useQuery(
    getCurrentTripOptions({ client: localClient, path: { carId } }),
  );
  const queued = useQueuedTrips(carId);
  const state = useMemo(() => tripState(data, queued), [data, queued]);
  return { ...state, isLoading: isLoading && !data };
}

/**
 * Starts and finishes trips. The id of a trip and the times come from the
 * device. Without a connection the request goes into the offline queue and is
 * sent as `queued` later: the server stores a trip that doesn't fit then with a
 * review reason instead of rejecting it.
 *
 * Throws the error of the server for a request it rejects (e.g. 409 with the
 * minimum odometer).
 */
export function useTripActions(carId: number) {
  const { account } = useMe();
  const addToQueue = useQueueRequest();

  const done = async () => {
    await invalidateCar(carId);
    return 'saved' as const;
  };

  const startQueued = async (id: string, body: TripStartInput) => {
    await addToQueue({
      id,
      carId,
      tripId: id,
      tripAction: 'start',
      url: startUrl(carId, id),
      body: { ...body, queued: true },
    });
    return 'queued' as const;
  };

  const finishQueued = async (id: string, body: TripFinishInput) => {
    await addToQueue({
      id: finishEntryId(id),
      carId,
      tripId: id,
      tripAction: 'finish',
      url: finishUrl(carId, id),
      body: { ...body, queued: true },
    });
    // the start may wait in the queue while the device is online again
    if (navigator.onLine && account?.name) {
      void offlineQueue.flush(account.name).then(({ sent }) => {
        if (sent.length) void invalidateCar(carId);
      });
    }
    return 'queued' as const;
  };

  const start = async (details: StartDetails): Promise<TripResult> => {
    const id = crypto.randomUUID();
    const body: TripStartInput = {
      ...details,
      startedAt: new Date().toISOString(),
    };
    if (!navigator.onLine) return startQueued(id, body);
    try {
      await startTrip({
        client: localClient,
        path: { carId, id },
        body: { ...body, queued: false },
        throwOnError: true,
      });
    } catch (error) {
      if (isOfflineError(error)) return startQueued(id, body);
      throw error;
    }
    return done();
  };

  /** a trip started offline is finished in the queue, after its start */
  const finish = async (
    trip: TripView,
    details: FinishDetails,
  ): Promise<TripResult> => {
    const body: TripFinishInput = {
      ...details,
      endedAt: new Date().toISOString(),
    };
    if (trip.pending || !navigator.onLine) return finishQueued(trip.id, body);
    try {
      await finishTrip({
        client: localClient,
        path: { carId, id: trip.id },
        body: { ...body, queued: false },
        throwOnError: true,
      });
    } catch (error) {
      if (isOfflineError(error)) return finishQueued(trip.id, body);
      throw error;
    }
    return done();
  };

  return { start, finish };
}
