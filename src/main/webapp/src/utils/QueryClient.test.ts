import { describe, expect, it, vi } from 'vitest';
import { getMyself } from '@/generated';
import { localClient } from './QueryClient';
import { BackendError } from './BackendError';

vi.mock('react-toastify', () => ({ toast: { error: vi.fn() } }));

const call = (fetch: typeof globalThis.fetch) =>
  getMyself({ client: localClient, fetch, throwOnError: true });

describe('localClient', () => {
  it('keeps the validation messages of a 400', async () => {
    const fetch = vi.fn(async () =>
      Response.json(['Title must be set!', 'Due date must be set!'], {
        status: 400,
      }),
    );

    await expect(call(fetch)).rejects.toEqual(
      new BackendError(400, 'Title must be set!\nDue date must be set!'),
    );
  });

  it('keeps the message and the details of a conflict', async () => {
    const conflict = {
      reason: 'ODOMETER_BELOW_PREVIOUS',
      message: 'The odometer is below 1000 km',
      minimumOdometer: 1000,
    };
    const fetch = vi.fn(async () => Response.json(conflict, { status: 409 }));

    await expect(call(fetch)).rejects.toEqual(
      new BackendError(409, 'The odometer is below 1000 km', conflict),
    );
  });

  it('reports the status of other errors', async () => {
    const fetch = vi.fn(async () => new Response('', { status: 404 }));

    await expect(call(fetch)).rejects.toEqual(new BackendError(404));
  });

  it('reports an unreachable server as status 0', async () => {
    const fetch = vi.fn(async () => {
      throw new TypeError('Failed to fetch');
    });

    await expect(call(fetch)).rejects.toEqual(new BackendError(0));
  });
});
