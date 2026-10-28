import type { ExpressionSpecification } from 'maplibre-gl';
import type { Feature, FeatureCollection, LineString, Point } from 'geojson';
import type { MapPoint } from './trackData';

/**
 * Speed is a magnitude: the plasma scale, orange (slow) over magenta and
 * violet to dark blue (fast). One hue alone was too faint on the map; plasma
 * changes the hue as well, keeps the lightness falling (the order reads without
 * color vision) and differs from the colors of the map. The steps are closer at
 * low speeds, where city driving is. The scale is fixed in km/h so that tracks
 * are comparable.
 */
export const SPEED_STOPS = [
  { kmh: 0, color: '#fca636' },
  { kmh: 30, color: '#e16462' },
  { kmh: 60, color: '#b12a90' },
  { kmh: 100, color: '#6a00a8' },
  { kmh: 160, color: '#0d0887' },
] as const;

export const MAX_SPEED = SPEED_STOPS[SPEED_STOPS.length - 1].kmh;

/** a piece of the track without a speed */
export const NO_SPEED_COLOR = '#8a8a85';

/** ticks of the legend, km/h */
export const LEGEND_TICKS = [0, 40, 80, 120, 160] as const;

/** color of a piece by its speed, for the paint of the line layer */
export const speedColor: ExpressionSpecification = [
  'case',
  ['==', ['typeof', ['get', 'speed']], 'number'],
  [
    'interpolate',
    ['linear'],
    ['get', 'speed'],
    ...SPEED_STOPS.flatMap((stop) => [stop.kmh, stop.color]),
  ],
  NO_SPEED_COLOR,
];

/** the CSS gradient of the legend, the same stops as the line */
export const legendGradient = `linear-gradient(to right, ${SPEED_STOPS.map(
  (stop) => `${stop.color} ${(stop.kmh / MAX_SPEED) * 100}%`,
).join(', ')})`;

/** steps of the speed that a line keeps, km/h */
const BAND = 10;

/** the speed of the piece to the point: the mean of its ends that have one */
function pieceSpeed(from: MapPoint, to: MapPoint): number | null {
  const speeds = [from.speed, to.speed].filter(
    (speed): speed is number => speed !== null,
  );
  return speeds.length
    ? speeds.reduce((sum, speed) => sum + speed, 0) / speeds.length
    : null;
}

const band = (speed: number | null) =>
  speed === null ? -1 : Math.floor(speed / BAND);

/**
 * The segments as lines of the same band of speed, each colored by the mean
 * speed of its pieces: a few thousand lines for a month instead of a feature
 * per pair of points, and the map can simplify them by its zoom. The segment
 * and the indexes of its points find the point under the pointer.
 */
export function trackBands(
  segments: MapPoint[][],
): FeatureCollection<LineString> {
  const features: Feature<LineString>[] = [];
  segments.forEach((segment, seg) => {
    let from = 0;
    while (from < segment.length - 1) {
      const first = band(pieceSpeed(segment[from], segment[from + 1]));
      let to = from + 1;
      const speeds = [pieceSpeed(segment[from], segment[to])];
      while (
        to < segment.length - 1 &&
        band(pieceSpeed(segment[to], segment[to + 1])) === first
      ) {
        speeds.push(pieceSpeed(segment[to], segment[to + 1]));
        to++;
      }
      const known = speeds.filter((speed): speed is number => speed !== null);
      features.push({
        type: 'Feature',
        geometry: {
          type: 'LineString',
          coordinates: segment
            .slice(from, to + 1)
            .map((point) => [point.lon, point.lat]),
        },
        properties: {
          seg,
          from,
          to,
          speed: known.length
            ? known.reduce((sum, speed) => sum + speed, 0) / known.length
            : null,
        },
      });
      from = to;
    }
  });
  return { type: 'FeatureCollection', features };
}

/** the first and the last point of every segment */
export function trackEnds(segments: MapPoint[][]): FeatureCollection<Point> {
  return {
    type: 'FeatureCollection',
    features: segments.flatMap((segment) => {
      const ends = [
        { point: segment[0], end: 'start' },
        { point: segment[segment.length - 1], end: 'end' },
      ];
      return ends.map(({ point, end }) => ({
        type: 'Feature' as const,
        geometry: {
          type: 'Point' as const,
          coordinates: [point.lon, point.lat],
        },
        properties: { end, ts: point.ts, speed: point.speed },
      }));
    }),
  };
}
