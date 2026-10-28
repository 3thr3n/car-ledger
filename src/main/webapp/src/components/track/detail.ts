import type { MapView } from './TrackMap';

/** meters of the earth's radius, as the server measures */
const EARTH_RADIUS = 6_371_000;
/** the shown part plus this much of its width and height on each side */
const MARGIN = 0.5;
/** a line may be this many pixels off before the detail is fetched */
const MAX_PIXELS = 2;

/** meters of a pixel in the middle of the view, tiles of 512 pixels */
export function metersPerPixel(view: MapView): number {
  const lat = (view.south + view.north) / 2;
  return (
    (2 * Math.PI * EARTH_RADIUS * Math.cos((lat * Math.PI) / 180)) /
    512 /
    2 ** view.zoom
  );
}

/**
 * Whether the lines of the whole period are visibly off at this zoom, e.g. two
 * drives of a street apart
 *
 * @param tolerance meters, of the track of the whole period
 */
export const needsDetail = (tolerance: number | undefined, view?: MapView) =>
  !!view &&
  tolerance !== undefined &&
  tolerance > MAX_PIXELS * metersPerPixel(view);

const round = (value: number) => Math.round(value * 10_000) / 10_000;
const clamp = (value: number, limit: number) =>
  Math.max(-limit, Math.min(limit, value));

/**
 * The query for the detail of the view: with a margin, so a small move needs
 * none at once, rounded, so the same view is one entry of the cache
 */
export function detailQuery(view: MapView): { bbox: string; zoom: number } {
  const width = view.east - view.west;
  const height = view.north - view.south;
  const bbox = [
    clamp(view.west - width * MARGIN, 180),
    clamp(view.south - height * MARGIN, 90),
    clamp(view.east + width * MARGIN, 180),
    clamp(view.north + height * MARGIN, 90),
  ].map(round);
  return { bbox: bbox.join(','), zoom: Math.round(view.zoom * 10) / 10 };
}
