import { BillType } from '@/generated';
import { OfflineStore } from './store';

/**
 * A request made without a connection, sent when there is one again. The id is
 * created on the device and sent as `Idempotency-Key`: a request sent again
 * after a lost answer doesn't create the entry twice.
 */
export interface QueuedRequest {
  id: string;
  /** the account that made it, only its login sends it */
  user: string;
  carId: number;
  /** of a new bill */
  billType?: BillType;
  /** of a trip: its id, which the server takes from the URL */
  tripId?: string;
  tripAction?: 'start' | 'finish';
  method: 'PUT';
  /** e.g. /api/bill/1/fuel or /api/trip/1/<id>/finish */
  url: string;
  /** in the units of the API */
  body: Record<string, unknown>;
  /** ISO time, the requests are sent in this order */
  createdAt: string;
  /** failed: the server rejected it, the user corrects or discards it */
  status: 'waiting' | 'failed';
  /** message of the server for a failed one */
  error?: string;
}

/** how a request was answered, without an answer the device is offline */
export type Send = (request: QueuedRequest) => Promise<Response>;

export interface FlushResult {
  sent: QueuedRequest[];
  failed: QueuedRequest[];
}

/**
 * No session: the login is needed, the queue waits for it. 0 is a redirect to
 * the login, which fetch doesn't follow.
 */
const LOGIN = [0, 401, 499];

/**
 * Validation errors are a json list of messages, conflicts a text or an object
 * with a message
 */
async function messageOf(response: Response): Promise<string> {
  const text = await response.text().catch(() => '');
  try {
    const parsed: unknown = JSON.parse(text);
    if (Array.isArray(parsed)) return parsed.map(String).join('\n');
    if (parsed && typeof parsed === 'object' && 'message' in parsed) {
      return String(parsed.message);
    }
  } catch {
    // plain text
  }
  return text;
}

/**
 * The offline queue of the device. Kept in the store, so it survives a reload
 * and the end of the app. Several tabs share it: they send one after the other
 * (Web Locks), and the id of a request protects against sending it twice.
 */
export class OfflineQueue {
  private entries: QueuedRequest[] = [];
  private listeners = new Set<() => void>();
  private loaded: Promise<void> | undefined;
  private flushing: Promise<FlushResult> | undefined;

  /**
   * @param onChange called when this queue changed the store, e.g. to tell
   *   the other tabs
   */
  constructor(
    private readonly store: OfflineStore,
    private readonly send: Send,
    private readonly onChange?: () => void,
  ) {}

  /** reads the store again, e.g. after another tab changed it */
  load(): Promise<void> {
    this.loaded = this.store.all<QueuedRequest>('queue').then((entries) => {
      this.entries = entries.sort((a, b) =>
        a.createdAt.localeCompare(b.createdAt),
      );
      this.notify();
    });
    return this.loaded;
  }

  private ready() {
    return this.loaded ?? this.load();
  }

  /** the same array until something changes, for useSyncExternalStore */
  list = (): QueuedRequest[] => this.entries;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  private notify() {
    this.listeners.forEach((listener) => listener());
  }

  private async save(entry: QueuedRequest) {
    await this.store.put('queue', entry.id, entry);
    this.entries = [...this.entries.filter((e) => e.id !== entry.id), entry];
    this.entries.sort((a, b) => a.createdAt.localeCompare(b.createdAt));
    this.notify();
    this.onChange?.();
  }

  /** adds a request, or replaces the one with the same id (corrected) */
  async add(entry: QueuedRequest) {
    await this.ready();
    await this.save(entry);
  }

  async discard(id: string) {
    await this.ready();
    await this.store.delete('queue', id);
    this.entries = this.entries.filter((e) => e.id !== id);
    this.notify();
    this.onChange?.();
  }

  /** e.g. of a deleted account */
  async discardUser(user: string) {
    await this.ready();
    for (const entry of this.entries.filter((e) => e.user === user)) {
      await this.discard(entry.id);
    }
  }

  get(id: string): QueuedRequest | undefined {
    return this.entries.find((e) => e.id === id);
  }

  /**
   * Sends the waiting requests of the user in the order they were made, until
   * the device is offline, the login is missing or the server has a problem.
   * A request the server rejects is kept as failed, the next ones are sent.
   */
  flush(user: string): Promise<FlushResult> {
    this.flushing ??= this.locked(() => this.sendAll(user)).finally(() => {
      this.flushing = undefined;
    });
    return this.flushing;
  }

  private locked<T>(task: () => Promise<T>): Promise<T> {
    const locks =
      typeof navigator !== 'undefined' ? navigator.locks : undefined;
    return locks ? locks.request('car-ledger-queue', task) : task();
  }

  private async sendAll(user: string): Promise<FlushResult> {
    // another tab may have sent or added requests
    await this.load();
    const result: FlushResult = { sent: [], failed: [] };
    const waiting = this.entries.filter(
      (e) => e.user === user && e.status === 'waiting',
    );
    for (const entry of waiting) {
      let response: Response;
      try {
        response = await this.send(entry);
      } catch {
        break; // offline
      }
      if (response.ok) {
        await this.discard(entry.id);
        result.sent.push(entry);
      } else if (LOGIN.includes(response.status) || response.status >= 500) {
        break; // the next attempt sends it
      } else {
        const failed: QueuedRequest = {
          ...entry,
          status: 'failed',
          error: await messageOf(response),
        };
        await this.save(failed);
        result.failed.push(failed);
      }
    }
    return result;
  }
}
