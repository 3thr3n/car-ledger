import { describe, expect, it } from 'vitest';
import { Trip, TripCurrent, TripType } from '@/generated';
import { QueuedRequest } from '@/offline/queue';
import { finishEntryId, tripState } from './tripState';

const serverTrip: Trip = {
  id: 'server-trip',
  carId: 1,
  type: TripType.BUSINESS,
  startOdometer: 1000,
  startedAt: '2026-10-19T08:00:00Z',
  restored: false,
  createdAt: '2026-10-19T08:00:01Z',
};

const current = (patch: Partial<TripCurrent> = {}): TripCurrent => ({
  minimumOdometer: 1000,
  estimatedOdometer: 1200,
  ...patch,
});

const start = (
  tripId: string,
  startOdometer: number,
  patch: Partial<QueuedRequest> = {},
): QueuedRequest => ({
  id: tripId,
  user: 'alice',
  carId: 1,
  tripId,
  tripAction: 'start',
  method: 'PUT',
  url: `/api/trip/1/${tripId}`,
  body: {
    type: TripType.PRIVATE,
    startOdometer,
    startedAt: '2026-10-19T09:00:00Z',
    destination: 'Lake',
    queued: true,
  },
  createdAt: '2026-10-19T09:00:00Z',
  status: 'waiting',
  ...patch,
});

const finish = (tripId: string, endOdometer: number): QueuedRequest => ({
  ...start(tripId, 0),
  id: finishEntryId(tripId),
  tripAction: 'finish',
  url: `/api/trip/1/${tripId}/finish`,
  body: { endOdometer, endedAt: '2026-10-19T10:00:00Z', queued: true },
  createdAt: '2026-10-19T10:00:00Z',
});

describe('tripState', () => {
  it('takes the state of the server without a queue', () => {
    expect(tripState(current({ open: serverTrip }), [])).toEqual({
      open: serverTrip,
      minimumOdometer: 1000,
      suggestedOdometer: 1200,
    });
  });

  it('suggests the minimum when the estimate is below it', () => {
    expect(
      tripState(current({ estimatedOdometer: 900 }), []).suggestedOdometer,
    ).toBe(1000);
  });

  it('opens a trip started offline, with the details of its start', () => {
    const state = tripState(current(), [start('offline-trip', 1300)]);

    expect(state.open).toMatchObject({
      id: 'offline-trip',
      type: TripType.PRIVATE,
      startOdometer: 1300,
      startedAt: '2026-10-19T09:00:00Z',
      destination: 'Lake',
      pending: true,
    });
    expect(state.minimumOdometer).toBe(1300);
    expect(state.suggestedOdometer).toBe(1300);
  });

  it('closes a trip finished offline and raises the minimum to its end', () => {
    const state = tripState(current({ open: serverTrip }), [
      finish('server-trip', 1450),
    ]);

    expect(state.open).toBeUndefined();
    expect(state.minimumOdometer).toBe(1450);
  });

  it('closes a trip started and finished offline', () => {
    const state = tripState(current(), [
      start('offline-trip', 1300),
      finish('offline-trip', 1350),
    ]);

    expect(state.open).toBeUndefined();
    expect(state.minimumOdometer).toBe(1350);
  });

  it('leaves out requests the server rejected', () => {
    const state = tripState(current(), [
      start('rejected-trip', 5000, { status: 'failed', error: 'no' }),
    ]);

    expect(state.open).toBeUndefined();
    expect(state.minimumOdometer).toBe(1000);
  });

  it('works without a state of the server, e.g. never loaded offline', () => {
    expect(tripState(undefined, [start('offline-trip', 1300)])).toMatchObject({
      open: { id: 'offline-trip' },
      minimumOdometer: 1300,
      suggestedOdometer: 1300,
    });
  });
});
