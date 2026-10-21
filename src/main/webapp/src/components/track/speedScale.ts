import type { ExpressionSpecification } from 'maplibre-gl';
import type { Feature, FeatureCollection, LineString, Point } from 'geojson';
import type { TrackPoint } from '@/generated';

/**
 * Speed is a magnitude: one hue from light (slow) to dark (fast), not a
 * rainbow, so the order reads without color vision. The scale is fixed in km/h
 * so that tracks are comparable. The lightest step stays visible on the map
 * with the white casing below the line.
 */
export const SPEED_STOPS = [
  { kmh: 0, color: '#86b6ef' },
  { kmh: 50, color: '#3987e5' },
  { kmh: 100, color: '#1c5cab' },
  { kmh: 160, color: '#0d366b' },
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

/**
 * Pieces between the points of the segments, each colored by the mean speed
 * of its two points (the one that is known, if only one is)
 */
export function trackPieces(
  segments: TrackPoint[][],
): FeatureCollection<LineString> {
  const features: Feature<LineString>[] = [];
  for (const segment of segments) {
    for (let i = 1; i < segment.length; i++) {
      const from = segment[i - 1];
      const to = segment[i];
      const speeds = [from.speed, to.speed].filter(
        (speed): speed is number => speed !== undefined && speed !== null,
      );
      features.push({
        type: 'Feature',
        geometry: {
          type: 'LineString',
          coordinates: [
            [from.lon, from.lat],
            [to.lon, to.lat],
          ],
        },
        properties: {
          ts: to.ts,
          speed: speeds.length
            ? speeds.reduce((sum, speed) => sum + speed, 0) / speeds.length
            : null,
        },
      });
    }
  }
  return { type: 'FeatureCollection', features };
}

/** the first and the last point of every segment */
export function trackEnds(segments: TrackPoint[][]): FeatureCollection<Point> {
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
        properties: { end, ts: point.ts, speed: point.speed ?? null },
      }));
    }),
  };
}
