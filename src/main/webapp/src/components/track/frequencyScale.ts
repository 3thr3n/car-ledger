import type { ExpressionSpecification } from 'maplibre-gl';
import type { Feature, FeatureCollection, LineString } from 'geojson';
import type { TrackFrequency } from '@/generated';

/**
 * How often is a magnitude as well, but not the speed: a heat scale from
 * yellow (once) to dark red (most often), the line wider as well, so the two
 * views can't be mistaken for each other. Unlike the speed the scale follows
 * the period: from once to the most passes in it.
 */
export const FREQUENCY_COLORS = [
  '#feb24c',
  '#fc4e2a',
  '#bd0026',
  '#800026',
] as const;

/** of the line, at once and at the most passes */
export const FREQUENCY_WIDTH = [3, 9] as const;

/** the top of the scale, at least 2 so that it has a range */
export const scaleMax = (maxPasses: number) => Math.max(2, maxPasses);

const stops = (max: number) =>
  FREQUENCY_COLORS.map(
    (_, i) => 1 + ((max - 1) * i) / (FREQUENCY_COLORS.length - 1),
  );

/** color of a line by its passes, for the paint of the line layer */
export function frequencyColor(maxPasses: number): ExpressionSpecification {
  const max = scaleMax(maxPasses);
  return [
    'interpolate',
    ['linear'],
    ['get', 'passes'],
    ...stops(max).flatMap((passes, i) => [passes, FREQUENCY_COLORS[i]]),
  ] as ExpressionSpecification;
}

/** width of a line by its passes */
export function frequencyWidth(maxPasses: number): ExpressionSpecification {
  return [
    'interpolate',
    ['linear'],
    ['get', 'passes'],
    1,
    FREQUENCY_WIDTH[0],
    scaleMax(maxPasses),
    FREQUENCY_WIDTH[1],
  ];
}

/** the CSS gradient of the legend, the same colors as the line */
export const frequencyGradient = `linear-gradient(to right, ${FREQUENCY_COLORS.join(', ')})`;

/** the lines as features, in their order: the most frequent on top */
export function frequencyLines(
  frequency: TrackFrequency,
): FeatureCollection<LineString> {
  return {
    type: 'FeatureCollection',
    features: frequency.lines.map((line): Feature<LineString> => ({
      type: 'Feature',
      geometry: { type: 'LineString', coordinates: line.coordinates },
      properties: { passes: line.passes, last: line.last },
    })),
  };
}
