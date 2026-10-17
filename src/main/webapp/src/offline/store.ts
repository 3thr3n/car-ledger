/**
 * Storage of the offline data on the device: the requests of the queue and the
 * data the forms need without a connection. IndexedDB in the browser, a map in
 * the unit tests.
 */
export interface OfflineStore {
  all<T>(table: Table): Promise<T[]>;
  get<T>(table: Table, key: string): Promise<T | undefined>;
  put<T>(table: Table, key: string, value: T): Promise<void>;
  delete(table: Table, key: string): Promise<void>;
}

/** `queue`: the waiting requests, `cache`: the kept data of the queries */
export type Table = 'queue' | 'cache';

const DB_NAME = 'car-ledger';
const DB_VERSION = 1;

function request<T>(req: IDBRequest<T>): Promise<T> {
  return new Promise((resolve, reject) => {
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

/** the database is opened once, on the first use */
export function indexedDbStore(): OfflineStore {
  let db: Promise<IDBDatabase> | undefined;
  const open = () => {
    db ??= new Promise((resolve, reject) => {
      const req = indexedDB.open(DB_NAME, DB_VERSION);
      req.onupgradeneeded = () => {
        req.result.createObjectStore('queue');
        req.result.createObjectStore('cache');
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
    return db;
  };
  const store = async (table: Table, mode: IDBTransactionMode) =>
    (await open()).transaction(table, mode).objectStore(table);

  return {
    all: async <T>(table: Table) =>
      request((await store(table, 'readonly')).getAll()) as Promise<T[]>,
    get: async <T>(table: Table, key: string) =>
      request((await store(table, 'readonly')).get(key)) as Promise<
        T | undefined
      >,
    put: async (table, key, value) => {
      await request((await store(table, 'readwrite')).put(value, key));
    },
    delete: async (table, key) => {
      await request((await store(table, 'readwrite')).delete(key));
    },
  };
}

export function memoryStore(): OfflineStore {
  const tables: Record<Table, Map<string, unknown>> = {
    queue: new Map(),
    cache: new Map(),
  };
  // copies, like IndexedDB stores a clone
  const clone = <T>(value: T): T =>
    value === undefined ? value : structuredClone(value);
  return {
    all: async <T>(table: Table) =>
      [...tables[table].values()].map(clone) as T[],
    get: async <T>(table: Table, key: string) =>
      clone(tables[table].get(key)) as T | undefined,
    put: async (table, key, value) => {
      tables[table].set(key, clone(value));
    },
    delete: async (table, key) => {
      tables[table].delete(key);
    },
  };
}
