-- What a car runs on, with the size of its tank and battery

-- existing cars were all refueled, petrol is the most common
ALTER TABLE car ADD COLUMN c_energy_type VARCHAR(20) NOT NULL DEFAULT 'PETROL';
-- liters, only for cars with a tank
ALTER TABLE car ADD COLUMN c_tank_size NUMERIC(5, 1);
-- kWh, only for cars with a battery
ALTER TABLE car ADD COLUMN c_battery_capacity NUMERIC(5, 1);
