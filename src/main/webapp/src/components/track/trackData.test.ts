import { describe, expect, it } from 'vitest';
import { decodeTrack, nearestPoint } from './trackData';

describe('decodeTrack', () => {
  it('turns the compact points into points with their time', () => {
    const segments = decodeTrack({
      segments: [
        {
          start: '2026-10-20T08:00:00Z',
          points: [
            [10, 50, 0, 42.5],
            [10.001, 50.002, 2.5, null as unknown as number],
          ],
        },
        { start: '2026-10-20T09:00:00.5Z', points: [[10, 50, 0, 0]] },
      ],
    });

    expect(segments).toEqual([
      [
        { ts: '2026-10-20T08:00:00.000Z', lon: 10, lat: 50, speed: 42.5 },
        {
          ts: '2026-10-20T08:00:02.500Z',
          lon: 10.001,
          lat: 50.002,
          speed: null,
        },
      ],
      [{ ts: '2026-10-20T09:00:00.500Z', lon: 10, lat: 50, speed: 0 }],
    ]);
  });
});

describe('nearestPoint', () => {
  const segment = [0, 1, 2, 3, 4].map((i) => ({
    ts: `2026-10-20T08:00:0${i}Z`,
    lat: 50 + i * 0.001,
    lon: 10,
    speed: i * 10,
  }));

  it('finds the point of the part nearest to the position', () => {
    expect(nearestPoint(segment, 0, 4, 10.0001, 50.0021)?.speed).toBe(20);
    // only within the part
    expect(nearestPoint(segment, 3, 4, 10, 50)?.speed).toBe(30);
    expect(nearestPoint(segment, 5, 9, 10, 50)).toBeUndefined();
  });
});
