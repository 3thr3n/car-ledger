import { describe, expect, it } from 'vitest';
import { gforceBands, gforceLevel, largest } from './gforceScale';
import type { GForcePoint, MapPoint } from './trackData';

const imu = (
  accel: number | null,
  brake: number | null,
  lateral: number | null,
): GForcePoint => ({ accel, brake, lateral, imu: true });

const point = (i: number, g: GForcePoint | null): MapPoint => ({
  ts: `2026-10-20T08:00:${String(i).padStart(2, '0')}Z`,
  lat: 50 + i * 0.001,
  lon: 10,
  speed: 50,
  g,
});

describe('largest', () => {
  it('takes the strongest force, cornering to either side', () => {
    expect(largest(imu(0.1, 0.3, -0.4))).toEqual({ g: 0.4, kind: 'lateral' });
    expect(largest(imu(0.2, null, null))).toEqual({ g: 0.2, kind: 'accel' });
    expect(largest(imu(null, null, null))).toBeNull();
    expect(largest(null)).toBeNull();
  });
});

describe('gforceLevel', () => {
  it('is harsh from a lower value of the GPS', () => {
    expect(gforceLevel(imu(0, 0.25, 0))).toBeCloseTo(0.5);
    expect(
      gforceLevel({ accel: 0, brake: 0.15, lateral: 0, imu: false }),
    ).toBeCloseTo(0.5);
    expect(gforceLevel(imu(0, 0.9, 0))).toBe(1);
    expect(gforceLevel(null)).toBeNull();
  });
});

describe('gforceBands', () => {
  it('colors the piece to a point by its peaks and keeps a short harsh one', () => {
    const calm = imu(0.05, 0, 0.05);
    const segment = [
      point(0, calm),
      point(1, calm),
      point(2, calm),
      point(3, imu(0, 0.6, 0)),
      point(4, calm),
      point(5, null),
    ];

    const { features } = gforceBands([segment]);

    expect(
      features.map((feature) => [
        feature.properties?.from,
        feature.properties?.to,
        feature.properties?.level,
      ]),
    ).toEqual([
      [0, 2, 0.1],
      // the braking is on the piece to its point
      [2, 3, 1],
      [3, 4, 0.1],
      [4, 5, null],
    ]);
  });
});
