import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BillType } from '@/generated';
import { OfflineQueue, QueuedRequest, Send } from './queue';
import { memoryStore, OfflineStore } from './store';

const request = (
  id: string,
  patch: Partial<QueuedRequest> = {},
): QueuedRequest => ({
  id,
  user: 'alice',
  carId: 1,
  billType: BillType.FUEL,
  method: 'PUT',
  url: '/api/bill/1/fuel',
  body: { date: '2026-10-18', total: 50 },
  createdAt: `2026-10-18T10:00:0${id}Z`,
  status: 'waiting',
  ...patch,
});

/** 0 like the redirect to the login, which fetch doesn't follow */
const answer = (status: number, body = '') =>
  status === 0 ? Response.error() : new Response(body || null, { status });

describe('offline queue', () => {
  let store: OfflineStore;
  let sent: string[];
  let answers: Record<string, () => Response>;
  let onChange: () => void;
  let queue: OfflineQueue;

  const send: Send = async (entry) => {
    sent.push(entry.id);
    return (answers[entry.id] ?? (() => answer(202)))();
  };

  beforeEach(() => {
    store = memoryStore();
    sent = [];
    answers = {};
    onChange = vi.fn();
    queue = new OfflineQueue(store, send, onChange);
  });

  const ids = () => queue.list().map((entry) => entry.id);

  it('sends the requests of the user in the order they were made', async () => {
    await queue.add(request('2'));
    await queue.add(request('1'));
    await queue.add(request('3', { user: 'bob' }));

    const result = await queue.flush('alice');

    expect(sent).toEqual(['1', '2']);
    expect(result.sent.map((entry) => entry.id)).toEqual(['1', '2']);
    // the one of bob waits for his login
    expect(ids()).toEqual(['3']);
  });

  it('stops while the device is offline and keeps the rest', async () => {
    await queue.add(request('1'));
    await queue.add(request('2'));
    answers['1'] = () => {
      throw new TypeError('Failed to fetch');
    };

    const result = await queue.flush('alice');

    expect(sent).toEqual(['1']);
    expect(result.sent).toEqual([]);
    expect(ids()).toEqual(['1', '2']);
    expect(queue.list().every((entry) => entry.status === 'waiting')).toBe(
      true,
    );
  });

  it.each([0, 401, 499, 500, 503])(
    'waits for the login or the server after a %i',
    async (status) => {
      await queue.add(request('1'));
      await queue.add(request('2'));
      answers['1'] = () => answer(status);

      await queue.flush('alice');

      expect(sent).toEqual(['1']);
      expect(queue.get('1')?.status).toBe('waiting');
    },
  );

  it('keeps a rejected request as failed and sends the next', async () => {
    await queue.add(request('1'));
    await queue.add(request('2'));
    answers['1'] = () =>
      answer(400, JSON.stringify(['Date must be set!', 'Vat rate too high']));

    const result = await queue.flush('alice');

    expect(sent).toEqual(['1', '2']);
    expect(result.failed.map((entry) => entry.id)).toEqual(['1']);
    expect(queue.get('1')).toMatchObject({
      status: 'failed',
      error: 'Date must be set!\nVat rate too high',
    });
    expect(ids()).toEqual(['1']);
  });

  it('sends a failed request only after it was corrected', async () => {
    await queue.add(request('1'));
    answers['1'] = () => answer(409, 'A bill for this car exists.');
    await queue.flush('alice');
    expect(queue.get('1')?.error).toBe('A bill for this car exists.');

    await queue.flush('alice');
    expect(sent).toEqual(['1']);

    delete answers['1'];
    await queue.add(request('1', { body: { date: '2026-10-17', total: 51 } }));
    await queue.flush('alice');
    expect(sent).toEqual(['1', '1']);
    expect(ids()).toEqual([]);
  });

  it('sends a request once when two flushes run at the same time', async () => {
    await queue.add(request('1'));

    await Promise.all([queue.flush('alice'), queue.flush('alice')]);

    expect(sent).toEqual(['1']);
  });

  it('keeps the requests in the store for the next start', async () => {
    await queue.add(request('1'));

    const next = new OfflineQueue(store, send);
    await next.load();

    expect(next.list().map((entry) => entry.id)).toEqual(['1']);
  });

  it('reads the requests another tab added before sending', async () => {
    await new OfflineQueue(store, send).add(request('1'));

    await queue.flush('alice');

    expect(sent).toEqual(['1']);
  });

  it('tells the other tabs about its own changes only', async () => {
    await queue.load();
    expect(onChange).not.toHaveBeenCalled();

    await queue.add(request('1'));
    await queue.discard('1');
    expect(onChange).toHaveBeenCalledTimes(2);
  });

  it('discards the requests of a deleted account', async () => {
    await queue.add(request('1'));
    await queue.add(request('2', { user: 'bob' }));

    await queue.discardUser('alice');

    expect(ids()).toEqual(['2']);
  });
});
