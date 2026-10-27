import {
  dehydrate,
  DehydratedState,
  hydrate,
  Query,
  QueryClient,
} from '@tanstack/react-query';
import { dropDocumentsExcept, forgetDocuments } from './documents';
import { OfflineStore } from './store';

/**
 * The queries a form needs without a connection: the account (units, VAT,
 * currency), the cars (energy type, tank size, odometer) and their open trip
 * with the minimum odometer of the next start, and the modifications with
 * their documents and the HUs with their reports to show them at a roadside
 * check. Not the lists of the bills
 * and trips.
 */
const KEPT = new Set([
  'getMyself',
  'getMyCars',
  'getMyCar',
  'getCurrentTrip',
  'getModifications',
  'getModificationDocuments',
  'getTechnicalInspections',
  'getTechnicalInspectionFiles',
]);
const KEY = 'queries';
/** writes at most once per this many ms */
const DELAY = 500;

const idOf = (query: Query) =>
  (query.queryKey[0] as { _id?: string } | undefined)?._id ?? '';

/** the car of a kept query, missing for the account and the list of the cars */
const carOf = (query: Query) => {
  const path = (
    query.queryKey[0] as { path?: { carId?: number; id?: number } } | undefined
  )?.path;
  return path?.carId ?? path?.id;
};

/** the account the kept queries belong to, of the restore or the first load */
let owner: string | undefined;

export const isKept = (query: Query) =>
  KEPT.has(idOf(query)) && query.state.status === 'success';

/** the kept queries of the last visit, before the first render */
export async function restoreQueries(client: QueryClient, store: OfflineStore) {
  try {
    const state = await store.get<DehydratedState>('cache', KEY);
    if (state) hydrate(client, state);
    owner = client
      .getQueryCache()
      .findAll({ predicate: (query) => idOf(query) === 'getMyself' })
      .map((query) => (query.state.data as { name?: string } | undefined)?.name)
      .find(Boolean);
  } catch {
    // without IndexedDB (e.g. a private window) the app works online only
  }
}

/**
 * Keeps the queries whenever one of them was loaded. They stay in memory for
 * the whole visit: each write has what is in memory, one removed after a few
 * unused minutes would be gone from the device with the next write.
 */
export function keepQueries(client: QueryClient, store: OfflineStore) {
  for (const id of KEPT) {
    client.setQueryDefaults([{ _id: id }], { gcTime: Infinity });
  }
  let timer: ReturnType<typeof setTimeout> | undefined;
  return client.getQueryCache().subscribe((event) => {
    if (
      event.type !== 'updated' ||
      event.action.type !== 'success' ||
      !KEPT.has(idOf(event.query))
    ) {
      return;
    }
    if (idOf(event.query) === 'getMyself') {
      followAccount(client, store, event.query.state.data);
    } else if (idOf(event.query) === 'getMyCars') {
      followCars(client, store, event.query.state.data);
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
 * Another account logged in on the device, e.g. after the session of the last
 * one expired without a logout: the queries and documents kept for that one
 * are dropped, the shown ones load again.
 */
function followAccount(
  client: QueryClient,
  store: OfflineStore,
  data: unknown,
) {
  const name = (data as { name?: string } | undefined)?.name;
  if (!name) return;
  if (owner && owner !== name) {
    void client.resetQueries({
      predicate: (query) => idOf(query) !== 'getMyself',
    });
    void forgetDocuments(store).catch(() => undefined);
  }
  owner = name;
}

/**
 * The kept queries and documents of the cars no longer in the list, e.g. the
 * access to a shared car ended or was revoked
 */
function followCars(client: QueryClient, store: OfflineStore, data: unknown) {
  if (!Array.isArray(data)) return;
  const carIds = new Set(data.map((car: { id?: number }) => car.id ?? -1));
  client.removeQueries({
    predicate: (query) => {
      const carId = carOf(query);
      return KEPT.has(idOf(query)) && carId !== undefined && !carIds.has(carId);
    },
  });
  void dropDocumentsExcept(store, carIds)
    .then((gone) =>
      gone.forEach((carId) =>
        client.removeQueries({ queryKey: ['offlineDocuments', carId] }),
      ),
    )
    .catch(() => undefined);
}

/**
 * On the logout: the next user of the device doesn't see them, nor the kept
 * documents
 */
export async function forgetQueries(store: OfflineStore) {
  owner = undefined;
  await store.delete('cache', KEY).catch(() => undefined);
  await forgetDocuments(store).catch(() => undefined);
}
