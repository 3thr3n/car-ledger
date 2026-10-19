-- The trip log is turned on per car, the Trips tab shows only then. Cars with trips keep it.
ALTER TABLE car ADD COLUMN c_trip_log BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE car SET c_trip_log = TRUE WHERE EXISTS (SELECT 1 FROM trip WHERE trip.tp_car_id = car.id);
