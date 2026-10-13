-- Optional details of a car: license plate, VIN and the purchase

ALTER TABLE car ADD COLUMN c_license_plate VARCHAR(20);
ALTER TABLE car ADD COLUMN c_vin VARCHAR(17);
ALTER TABLE car ADD COLUMN c_purchase_date DATE;
ALTER TABLE car ADD COLUMN c_purchase_price NUMERIC(12, 2);
