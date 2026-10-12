-- The odometer of a car is a reading on a date, refuels after it are added to
-- get the current odometer

ALTER TABLE car ADD COLUMN c_odometer_date DATE;

-- the odometer was the mileage when the tracking started, so before the first bill
UPDATE car
SET c_odometer_date = COALESCE((SELECT MIN(b_date) FROM bill WHERE b_car_id = car.id) - 1, CURRENT_DATE);

ALTER TABLE car ALTER COLUMN c_odometer_date SET NOT NULL;
ALTER TABLE car ALTER COLUMN c_odometer_date SET DEFAULT CURRENT_DATE;
