import type { Track } from '@/generated';

/** the g-forces of a point: the peaks since the point before, in g */
export interface GForcePoint {
  accel: number | null;
  brake: number | null;
  /** + turning right, - turning left */
  lateral: number | null;
  /** real peaks of the IMU, else averages of the GPS */
  imu: boolean;
}

/** a point of the track on the map */
export interface MapPoint {
  /** ISO-8601 */
  ts: string;
  lat: number;
  lon: number;
  /** km/h, null without */
  speed: number | null;
  /** only when asked for, null without */
  g?: GForcePoint | null;
}

/**
 * The segments of the compact track of the server: each point is
 * [lon, lat, seconds since the start of its segment, speed or null], with the
 * g-forces also [accel, brake, lateral, imu 1 or 0], all null without
 */
export function decodeTrack(track: Pick<Track, 'segments'>): MapPoint[][] {
  return track.segments.map((segment) => {
    const start = Date.parse(segment.start);
    return segment.points.map(
      ([lon, lat, seconds, speed, accel, brake, lateral, imu]) => {
        const point: MapPoint = {
          ts: new Date(start + Math.round(seconds * 1000)).toISOString(),
          lat,
          lon,
          speed: speed ?? null,
        };
        if (imu !== undefined) {
          point.g =
            imu === null
              ? null
              : {
                  accel: accel ?? null,
                  brake: brake ?? null,
                  lateral: lateral ?? null,
                  imu: imu === 1,
                };
        }
        return point;
      },
    );
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
