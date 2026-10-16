-- Units the values are shown and entered in, stored are always km and liters

ALTER TABLE account ADD COLUMN unit_system VARCHAR(10) NOT NULL DEFAULT 'METRIC';
