import { QueryClient } from '@tanstack/react-query';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { forgetQueries, keepQueries, restoreQueries } from './persist';
import { memoryStore, OfflineStore } from './store';

const key = (_id: string, path?: object) => [{ _id, path }];

describe('queries kept for offline', () => {
  let store: OfflineStore;

  beforeEach(() => {
    vi.useFakeTimers();
    store = memoryStore();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('keeps the account and the cars, not the bills', async () => {
    const client = new QueryClient();
    keepQueries(client, store);

    client.setQueryData(key('getMyself'), { name: 'alice' });
    client.setQueryData(key('getMyCar', { id: 1 }), { id: 1, name: 'Golf' });
    client.setQueryData(key('getAllFuelBills', { carId: 1 }), { total: 3 });
    await vi.runAllTimersAsync();

    const next = new QueryClient();
    await restoreQueries(next, store);

    expect(next.getQueryData(key('getMyself'))).toEqual({ name: 'alice' });
    expect(next.getQueryData(key('getMyCar', { id: 1 }))).toEqual({
      id: 1,
      name: 'Golf',
    });
    expect(next.getQueryData(key('getAllFuelBills', { carId: 1 }))).toBe(
      undefined,
    );
  });

  it('forgets them on the logout', async () => {
    const client = new QueryClient();
    keepQueries(client, store);
    client.setQueryData(key('getMyself'), { name: 'alice' });
    await vi.runAllTimersAsync();

    await forgetQueries(store);

    const next = new QueryClient();
    await restoreQueries(next, store);
    expect(next.getQueryData(key('getMyself'))).toBe(undefined);
  });

  it('keeps a list that was not opened in this visit', async () => {
    // the last visit opened the modifications
    const before = new QueryClient();
    keepQueries(before, store);
    before.setQueryData(key('getModifications', { carId: 1 }), [{ id: 1 }]);
    await vi.runAllTimersAsync();

    // this one doesn't, the cars are loaded again after a while; in the
    // browser an unused query is removed after 5 minutes, in node never
    const now = new QueryClient({
      defaultOptions: { queries: { gcTime: 5 * 60_000 } },
    });
    keepQueries(now, store);
    await restoreQueries(now, store);
    await vi.advanceTimersByTimeAsync(10 * 60_000);
    now.setQueryData(key('getMyCars'), [{ id: 1 }]);
    await vi.advanceTimersByTimeAsync(1000);

    const next = new QueryClient();
    await restoreQueries(next, store);
    expect(next.getQueryData(key('getModifications', { carId: 1 }))).toEqual([
      { id: 1 },
    ]);
  });
});
