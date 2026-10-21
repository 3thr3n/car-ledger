import { describe, expect, it } from 'vitest';
import { trackEnds, trackPieces } from './speedScale';

const point = (second: number, speed?: number) => ({
  ts: `2026-10-20T08:00:${String(second).padStart(2, '0')}Z`,
  lat: 50 + second / 1000,
  lon: 10,
  speed,
});

describe('trackPieces', () => {
  it('colors a piece by the mean speed of its points', () => {
    const pieces = trackPieces([[point(0, 40), point(1, 60), point(2)]]);

    expect(pieces.features).toHaveLength(2);
    expect(pieces.features[0].properties).toEqual({
      ts: point(1).ts,
      speed: 50,
    });
    // only one point has a speed
    expect(pieces.features[1].properties?.speed).toBe(60);
    expect(pieces.features[0].geometry.coordinates).toEqual([
      [10, 50],
      [10, 50.001],
    ]);
  });

  it('has no speed without one at both points', () => {
    const pieces = trackPieces([[point(0), point(1)]]);

    expect(pieces.features[0].properties?.speed).toBeNull();
  });

  it('does not connect the segments', () => {
    const pieces = trackPieces([[point(0, 10), point(1, 10)], [point(9, 10)]]);

    expect(pieces.features).toHaveLength(1);
  });
});

describe('trackEnds', () => {
  it('marks the start and the end of every segment', () => {
    const ends = trackEnds([[point(0), point(1)], [point(9)]]);

    expect(ends.features.map((feature) => feature.properties?.end)).toEqual([
      'start',
      'end',
      'start',
      'end',
    ]);
  });
});
