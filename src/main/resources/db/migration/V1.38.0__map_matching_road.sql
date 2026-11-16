-- The road between a matched track point and the one before (#604): the vertices of the road network as an encoded
-- polyline with 6 decimals (the format of Valhalla), so the map follows the curves instead of cutting them.
-- NULL on a straight piece, for the first point of a drive and after a point without a road.
ALTER TABLE track_point
    ADD COLUMN tk_match_road TEXT;

-- the matched points get their road with the next run of the matching; only points matched before, so a matching
-- service is set up
UPDATE track_point SET tk_match = 'P' WHERE tk_match = 'M';
