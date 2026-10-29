import type { Track } from '@/generated';

/** a point of the track on the map */
export interface MapPoint {
  /** ISO-8601 */
  ts: string;
  lat: number;
  lon: number;
  /** km/h, null without */
  speed: number | null;
}

/**
 * The segments of the compact track of the server: each point is
 * [lon, lat, seconds since the start of its segment, speed or null]
 */
export function decodeTrack(track: Pick<Track, 'segments'>): MapPoint[][] {
  return track.segments.map((segment) => {
    const start = Date.parse(segment.start);
    return segment.points.map(([lon, lat, seconds, speed]) => ({
      ts: new Date(start + Math.round(seconds * 1000)).toISOString(),
      lat,
      lon,
      speed: speed ?? null,
    }));
  });
}

/**
 * The point of a part of a segment nearest to a position, e.g. under the
 * pointer on a line of many points
 *
 * @param from first index of the part
 * @param to last index of the part
 */
export function nearestPoint(
  segment: MapPoint[],
  from: number,
  to: number,
  lon: number,
  lat: number,
): MapPoint | undefined {
  // degrees of longitude are shorter away from the equator
  const scale = Math.cos((lat * Math.PI) / 180);
  let nearest: MapPoint | undefined;
  let best = Infinity;
  for (let i = Math.max(0, from); i <= Math.min(to, segment.length - 1); i++) {
    const point = segment[i];
    const distance = ((point.lon - lon) * scale) ** 2 + (point.lat - lat) ** 2;
    if (distance < best) {
      best = distance;
      nearest = point;
    }
  }
  return nearest;
}
