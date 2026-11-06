import type { ExpressionSpecification } from 'maplibre-gl';
import type { Feature, FeatureCollection, LineString } from 'geojson';
import type { GForcePoint, MapPoint } from './trackData';

/**
 * Harsh from this g on: the IMU measures real peaks, the GPS only averages
 * over a second, noticeably lower for the same drive. Each source gets its own
 * range of the same colors, so a period with both doesn't look calmer only
 * because of the GPS. The GPS range is a guess, to tune with real drives.
 */
export const HARSH_IMU = 0.5;
export const HARSH_GPS = 0.3;

/**
 * From calm (blue) over yellow to harsh (red), by the level 0 to 1 of the
 * range of the source: different hues than the speed, red reads as harsh.
 */
export const GFORCE_STOPS = [
  { level: 0, color: '#4575b4' },
  { level: 0.25, color: '#91bfdb' },
  { level: 0.5, color: '#fee090' },
  { level: 0.75, color: '#fc8d59' },
  { level: 1, color: '#d73027' },
] as const;

/** a piece without g-forces, e.g. of a firmware before them */
export const NO_GFORCE_COLOR = '#8a8a85';

/** color of a piece by its level, for the paint of the line layer */
export const gforceColor: ExpressionSpecification = [
  'case',
  ['==', ['typeof', ['get', 'level']], 'number'],
  [
    'interpolate',
    ['linear'],
    ['get', 'level'],
    ...GFORCE_STOPS.flatMap((stop) => [stop.level, stop.color]),
  ],
  NO_GFORCE_COLOR,
];

/** the CSS gradient of the legend, the same stops as the line */
export const gforceGradient = `linear-gradient(to right, ${GFORCE_STOPS.map(
  (stop) => `${stop.color} ${stop.level * 100}%`,
).join(', ')})`;

export type GForceKind = 'accel' | 'brake' | 'lateral';

/** the largest of the g-forces and which one it is, null without any */
export function largest(
  g: GForcePoint | null | undefined,
): { g: number; kind: GForceKind } | null {
  if (!g) return null;
  const values: { g: number; kind: GForceKind }[] = [];
  if (g.accel !== null) values.push({ g: g.accel, kind: 'accel' });
  if (g.brake !== null) values.push({ g: g.brake, kind: 'brake' });
  if (g.lateral !== null)
    values.push({ g: Math.abs(g.lateral), kind: 'lateral' });
  return values.length
    ? values.reduce((top, other) => (other.g > top.g ? other : top))
    : null;
}

/**
 * 0 (calm) to 1 (harsh or more) in the range of the source, null without
 * g-forces
 */
export function gforceLevel(g: GForcePoint | null | undefined): number | null {
  const top = largest(g);
  if (!top || !g) return null;
  return Math.min(1, top.g / (g.imu ? HARSH_IMU : HARSH_GPS));
}

/** steps of the level that a line keeps */
const BANDS = 10;

const band = (level: number | null) =>
  level === null ? -1 : Math.min(BANDS - 1, Math.floor(level * BANDS));

/**
 * The segments as lines of the same band of g-force, like the speeds: the
 * g-forces of a point are the peaks since the point before, so they color the
 * piece to it. A line takes the highest level of its pieces, a short harsh
 * braking is a line of its own and isn't averaged away.
 */
export function gforceBands(
  segments: MapPoint[][],
): FeatureCollection<LineString> {
  const features: Feature<LineString>[] = [];
  segments.forEach((segment, seg) => {
    let from = 0;
    while (from < segment.length - 1) {
      const first = band(gforceLevel(segment[from + 1].g));
      let to = from + 1;
      let level = gforceLevel(segment[to].g);
      while (
        to < segment.length - 1 &&
        band(gforceLevel(segment[to + 1].g)) === first
      ) {
        const next = gforceLevel(segment[to + 1].g);
        if (next !== null && (level === null || next > level)) level = next;
        to++;
      }
      features.push({
        type: 'Feature',
        geometry: {
          type: 'LineString',
          coordinates: segment
            .slice(from, to + 1)
            .map((point) => [point.lon, point.lat]),
        },
        properties: { seg, from, to, level },
      });
      from = to;
    }
  });
  return { type: 'FeatureCollection', features };
}
