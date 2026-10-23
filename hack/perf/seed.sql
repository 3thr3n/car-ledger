-- Track points of a year for the measurements of hack/perf/measure.cjs, written straight into the database of the dev
-- mode (much faster than the ingest). Every day two commutes of 30 minutes at 50 km/h, 25 km north and back, each with
-- 10 minutes parked afterwards while the tracker keeps sending (GPS drift of a few meters). On the last
-- :always_on_days days the tracker sends all day, the worst case for the suggested trips and the map.
--
--   psql -v car=1 -v device=1 -v days=365 -v always_on_days=7 -f seed.sql
\set ON_ERROR_STOP on

-- meters to degrees of latitude
\set lat_per_m '(1.0 / 111195)'

WITH day AS (SELECT date_trunc('day', now()) - d * interval '1 day' AS start, d
             FROM generate_series(1, :days) d),
     commute AS (SELECT * FROM (VALUES (interval '7 hours 30 minutes', 0.0, 1.0),
                                       (interval '17 hours', 25000.0, -1.0)) c(leave, origin, direction)),
     point AS (SELECT day.start + commute.leave + make_interval(secs => s) AS ts,
                      CASE
                          WHEN s < 1800 THEN commute.origin + commute.direction * s * 13.9
                          -- parked at the end, drifting
                          ELSE commute.origin + commute.direction * 1800 * 13.9 + sin(s) * 8
                          END AS north,
                      CASE WHEN s < 1800 THEN 50.0 ELSE 0.0 END AS speed
               FROM day,
                    commute,
                    generate_series(0, 2399) s)
INSERT
INTO track_point (tk_device_id, tk_car_id, tk_ts, tk_lat, tk_lon, tk_speed, tk_alt, tk_hdop)
SELECT :device, :car, ts, 50 + north * :lat_per_m, 10 + cos(extract(epoch FROM ts)) * 5 * :lat_per_m, speed, 250, 1.0
FROM point
ON CONFLICT (tk_device_id, tk_ts) DO NOTHING;

-- the tracker on all day: parked at home or at work between the commutes
WITH second AS (SELECT date_trunc('day', now()) - d * interval '1 day' + make_interval(secs => s) AS ts, s
                FROM generate_series(1, :always_on_days) d,
                     generate_series(0, 86399) s)
INSERT
INTO track_point (tk_device_id, tk_car_id, tk_ts, tk_lat, tk_lon, tk_speed, tk_alt, tk_hdop)
SELECT :device,
       :car,
       ts,
       50 + (CASE WHEN s BETWEEN 27000 AND 61200 THEN 25020 ELSE -20 END + sin(s) * 8) * :lat_per_m,
       10,
       0,
       250,
       1.0
FROM second
WHERE ts < now()
ON CONFLICT (tk_device_id, tk_ts) DO NOTHING;

ANALYZE track_point;

SELECT count(*)                                                     AS points,
       pg_size_pretty(pg_total_relation_size('track_point'))      AS table_with_indexes
FROM track_point
WHERE tk_car_id = :car;
