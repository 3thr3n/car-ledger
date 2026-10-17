import {
  dehydrate,
  DehydratedState,
  hydrate,
  Query,
  QueryClient,
} from '@tanstack/react-query';
import { OfflineStore } from './store';

/**
 * The queries a form needs without a connection: the account (units, VAT,
 * currency) and the cars (energy type, tank size, odometer). Not the lists of
 * the bills.
 */
const KEPT = new Set(['getMyself', 'getMyCars', 'getMyCar']);
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

/** on the logout: the next user of the device doesn't see them */
export function forgetQueries(store: OfflineStore) {
  return store.delete('cache', KEY).catch(() => undefined);
}
