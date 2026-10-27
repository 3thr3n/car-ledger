import { Attachment, DocumentKind } from '@/generated';
import { OfflineStore } from './store';

/**
 * The documents of the modifications and the reports of the HU kept on the
 * device, e.g. for a roadside check without a connection. The files are in the Cache Storage, where the
 * service worker finds them as well; the store has what was kept per car.
 */
export const DOCUMENTS_CACHE = 'modification-documents';
/** per car, the papers come first, then the photos */
export const MAX_OFFLINE_BYTES = 50 * 1024 * 1024;
const KEY = 'documents';

export interface OfflineDocuments {
  /** ISO time of the last update */
  savedAt: string;
  /** addresses of the kept files */
  urls: string[];
  /** names of the files beyond {@link MAX_OFFLINE_BYTES} */
  skipped: string[];
}

type Kept = Record<number, OfflineDocuments>;

/** the Cache Storage only exists in a secure context */
export const canKeepDocuments = () => typeof caches !== 'undefined';

/**
 * @return the papers of the modifications first, then the reports of the HU,
 *         then the photos and the rest: what doesn't fit stays out
 */
export function inPriority(documents: Attachment[], reports: Attachment[]) {
  const of = (kind: DocumentKind) =>
    documents.filter((document) => document.kind === kind);
  return [
    ...of(DocumentKind.APPROVAL),
    ...of(DocumentKind.INSPECTION),
    ...reports,
    ...of(DocumentKind.PHOTO),
    ...of(DocumentKind.OTHER),
  ];
}

export async function keptDocuments(store: OfflineStore, carId: number) {
  return (await store.get<Kept>('cache', KEY))?.[carId];
}

async function setKept(
  store: OfflineStore,
  carId: number,
  documents?: OfflineDocuments,
) {
  const kept = { ...(await store.get<Kept>('cache', KEY)) };
  if (documents) {
    kept[carId] = documents;
  } else {
    delete kept[carId];
  }
  await store.put('cache', KEY, kept);
}

/**
 * Keeps the documents in their order until the limit (see {@link inPriority}),
 * loads the missing ones and drops the ones no longer wanted. A document never changes,
 * a kept one isn't loaded again.
 *
 * @param url address of the content of a document
 * @param load the content, with the session
 */
export async function keepDocuments(
  store: OfflineStore,
  cache: Cache,
  carId: number,
  documents: Attachment[],
  url: (document: Attachment) => string,
  load: (url: string) => Promise<Response>,
): Promise<OfflineDocuments> {
  const urls: string[] = [];
  const skipped: string[] = [];
  let bytes = 0;
  for (const document of documents) {
    const size = document.size ?? 0;
    if (bytes + size > MAX_OFFLINE_BYTES) {
      skipped.push(document.fileName ?? '');
      continue;
    }
    bytes += size;
    urls.push(url(document));
  }

  for (const each of urls) {
    if (await cache.match(each)) continue;
    const response = await load(each);
    if (!response.ok) throw new Error(`${each}: ${response.status}`);
    await cache.put(each, response);
  }
  const before = (await keptDocuments(store, carId))?.urls ?? [];
  for (const each of before.filter((old) => !urls.includes(old))) {
    await cache.delete(each);
  }

  const kept = { savedAt: new Date().toISOString(), urls, skipped };
  await setKept(store, carId, kept);
  return kept;
}

/** the car no longer keeps its documents */
export async function dropDocuments(
  store: OfflineStore,
  cache: Cache,
  carId: number,
) {
  for (const each of (await keptDocuments(store, carId))?.urls ?? []) {
    await cache.delete(each);
  }
  await setKept(store, carId, undefined);
}

/**
 * The documents of the cars no longer in the list, e.g. the access to a shared
 * car ended
 *
 * @param carIds the cars the account may still use
 */
export async function dropDocumentsExcept(
  store: OfflineStore,
  carIds: Set<number>,
) {
  const kept = (await store.get<Kept>('cache', KEY)) ?? {};
  const gone = Object.keys(kept)
    .map(Number)
    .filter((carId) => !carIds.has(carId));
  const cache = canKeepDocuments()
    ? await caches.open(DOCUMENTS_CACHE)
    : undefined;
  for (const carId of gone) {
    if (cache) {
      await dropDocuments(store, cache, carId);
    } else {
      await setKept(store, carId, undefined);
    }
  }
  return gone;
}

/** on the logout: the next user of the device doesn't see them */
export async function forgetDocuments(store: OfflineStore) {
  await store.delete('cache', KEY);
  if (canKeepDocuments()) await caches.delete(DOCUMENTS_CACHE);
}
