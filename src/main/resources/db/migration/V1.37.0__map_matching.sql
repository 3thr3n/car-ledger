-- The position of a track point snapped to the road network (#504), only for the map: the raw position stays.
-- tk_match: P waits for the matching, M matched (tk_match_lat/lon set), U no road near it (the raw position is used),
-- NULL not to be matched (without a matching service, or before it was set up).
ALTER TABLE track_point
    ADD COLUMN tk_match     CHAR(1),
    ADD COLUMN tk_match_lat DOUBLE PRECISION,
    ADD COLUMN tk_match_lon DOUBLE PRECISION;

-- only the waiting points, small also on a large table
CREATE INDEX track_point_match_pending_idx ON track_point (tk_car_id, tk_ts) WHERE tk_match = 'P';
