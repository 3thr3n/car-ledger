-- Own picture of a car, an attachment of the car without a bill
ALTER TABLE car ADD COLUMN c_picture_id BIGINT REFERENCES attachment (id) ON DELETE SET NULL;
