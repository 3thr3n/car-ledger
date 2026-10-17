import { baseUrl } from '@/utils/QueryClient';
import { BackendError } from '@/utils/BackendError';
import { OfflineQueue, QueuedRequest } from './queue';
import { indexedDbStore, memoryStore } from './store';

/** IndexedDB, in a browser without it the queue lives until the reload */
export const offlineStore =
  typeof indexedDB === 'undefined' ? memoryStore() : indexedDbStore();

/**
 * Not with the generated client: it sends the browser to the login when the
 * session expired, the queue waits for the next login instead
 */
const send = (request: QueuedRequest) =>
  fetch(`${baseUrl}${request.url}`, {
    method: request.method,
    credentials: 'include',
    redirect: 'manual',
    headers: {
      'Content-Type': 'application/json',
      'X-Requested-With': 'JavaScript',
      'Idempotency-Key': request.id,
    },
    body: JSON.stringify(request.body),
  });

// the tabs of the app read the queue again when another one changed it
const channel =
  typeof BroadcastChannel === 'undefined'
    ? undefined
    : new BroadcastChannel('car-ledger-queue');

export const offlineQueue = new OfflineQueue(offlineStore, send, () =>
  channel?.postMessage('changed'),
);

if (channel) {
  channel.onmessage = () => void offlineQueue.load();
}

/** a request without an answer: the device is offline */
export const isOfflineError = (error: unknown) =>
  error instanceof BackendError && error.status === 0;
