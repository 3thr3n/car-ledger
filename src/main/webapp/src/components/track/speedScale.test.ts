import { describe, expect, it } from 'vitest';
import { trackBands, trackEnds } from './speedScale';

const point = (second: number, speed: number | null = null) => ({
  ts: `2026-10-20T08:00:${String(second).padStart(2, '0')}Z`,
  lat: 50 + second / 1000,
  lon: 10,
  speed,
});

describe('trackBands', () => {
  it('joins the pieces of a band of speed into one line', () => {
    const bands = trackBands([
      [point(0, 40), point(1, 44), point(2, 48), point(3, 70), point(4, 72)],
    ]);

    // 42, 46 (band 40), 59 (band 50), 71 (band 70)
    expect(bands.features.map((f) => f.properties)).toEqual([
      { seg: 0, from: 0, to: 2, speed: 44 },
      { seg: 0, from: 2, to: 3, speed: 59 },
      { seg: 0, from: 3, to: 4, speed: 71 },
    ]);
    expect(bands.features[0].geometry.coordinates).toEqual([
      [10, 50],
      [10, 50.001],
      [10, 50.002],
    ]);
  });

  it('uses the known speed, and none without one at both points', () => {
    const bands = trackBands([[point(0), point(1), point(2, 60)]]);

    expect(bands.features.map((f) => f.properties?.speed)).toEqual([null, 60]);
  });

  it('does not connect the segments', () => {
    const bands = trackBands([[point(0, 10), point(1, 10)], [point(9, 10)]]);

    expect(bands.features).toHaveLength(1);
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
