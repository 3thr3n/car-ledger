import { useMemo, useSyncExternalStore } from 'react';
import { BillType } from '@/generated';
import useMe from '@/hooks/useMe';
import { offlineQueue } from '.';
import { QueuedRequest } from './queue';

/**
 * The requests of the queue of the logged-in user, optionally of one car and
 * type, in the order they were made
 */
export function useQueued(carId?: number, billType?: BillType) {
  const { account } = useMe();
  const entries = useSyncExternalStore(
    offlineQueue.subscribe,
    offlineQueue.list,
  );
  return useMemo(
    () =>
      entries.filter(
        (entry) =>
          entry.user === account?.name &&
          (carId === undefined || entry.carId === carId) &&
          (billType === undefined || entry.billType === billType),
      ),
    [entries, account?.name, carId, billType],
  );
}

/** the requests that other accounts of this device left in the queue */
export function useOthersQueued() {
  const { account } = useMe();
  const entries = useSyncExternalStore(
    offlineQueue.subscribe,
    offlineQueue.list,
  );
  return useMemo(
    () => entries.filter((entry) => entry.user !== account?.name),
    [entries, account?.name],
  );
}

/** puts a new bill into the queue, sent when there is a connection again */
export function useQueueBill() {
  const { account } = useMe();
  return (
    entry: Pick<QueuedRequest, 'id' | 'carId' | 'billType' | 'url' | 'body'>,
  ) => {
    // asks the browser to keep the data of the app, e.g. Safari deletes it
    // after some weeks without a visit. A browser may decline.
    void navigator.storage?.persist?.().catch(() => false);
    return offlineQueue.add({
      ...entry,
      user: account?.name ?? '',
      method: 'PUT',
      createdAt: new Date().toISOString(),
      status: 'waiting',
    });
  };
}
