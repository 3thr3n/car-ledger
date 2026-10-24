-- Days the GPS track points of a car are kept, a nightly cleanup deletes older ones. NULL keeps them forever, only
-- then they are in the backup.
ALTER TABLE car
    ADD COLUMN c_track_retention_days INTEGER CHECK (c_track_retention_days > 0);
