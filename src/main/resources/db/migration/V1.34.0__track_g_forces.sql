-- The g-forces of a track point (#489): the peaks since the point before, in g. accel and brake are 0 or more, lateral
-- is + turning right and - turning left. Missing for older firmware. gsource says where they come from: imu (real
-- peaks), gps (averages over a second, lower) or unknown.

ALTER TABLE track_point
    ADD COLUMN tk_accel   DOUBLE PRECISION,
    ADD COLUMN tk_brake   DOUBLE PRECISION,
    ADD COLUMN tk_lateral DOUBLE PRECISION,
    ADD COLUMN tk_gsource VARCHAR(10);
