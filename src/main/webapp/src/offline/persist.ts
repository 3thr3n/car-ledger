import {
  dehydrate,
  DehydratedState,
  hydrate,
  Query,
  QueryClient,
} from '@tanstack/react-query';
import { forgetDocuments } from './documents';
import { OfflineStore } from './store';

/**
 * The queries a form needs without a connection: the account (units, VAT,
 * currency), the cars (energy type, tank size, odometer) and their open trip
 * with the minimum odometer of the next start, and the modifications with
 * their documents to show them at a roadside check. Not the lists of the bills
 * and trips.
 */
const KEPT = new Set([
  'getMyself',
  'getMyCars',
  'getMyCar',
  'getCurrentTrip',
  'getModifications',
  'getModificationDocuments',
]);
const KEY = 'queries';
/** writes at most once per this many ms */
const DELAY = 500;

const idOf = (query: Query) =>
  (query.queryKey[0] as { _id?: string } | undefined)?._id ?? '';

export const isKept = (query: Query) =>
  KEPT.has(idOf(query)) && query.state.status === 'success';

/** the kept queries of the last visit, before the first render */
export async function restoreQueries(client: QueryClient, store: OfflineStore) {
  try {
    const state = await store.get<DehydratedState>('cache', KEY);
    if (state) hydrate(client, state);
  } catch {
    // without IndexedDB (e.g. a private window) the app works online only
  }
}

/** keeps the queries whenever one of them was loaded */
export function keepQueries(client: QueryClient, store: OfflineStore) {
  let timer: ReturnType<typeof setTimeout> | undefined;
  return client.getQueryCache().subscribe((event) => {
    if (
      event.type !== 'updated' ||
      event.action.type !== 'success' ||
      !KEPT.has(idOf(event.query))
    ) {
      return;
    }
    clearTimeout(timer);
    timer = setTimeout(() => {
      store
        .put('cache', KEY, dehydrate(client, { shouldDehydrateQuery: isKept }))
        .catch(() => undefined);
    }, DELAY);
  });
}

/**
 * On the logout: the next user of the device doesn't see them, nor the kept
 * documents
 */
export async function forgetQueries(store: OfflineStore) {
  await store.delete('cache', KEY).catch(() => undefined);
  await forgetDocuments(store).catch(() => undefined);
}
