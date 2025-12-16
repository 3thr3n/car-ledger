-- V2.0.1__prefix_column_names.sql

ALTER TABLE if EXISTS Car RENAME COLUMN _odometer TO c_odometer;
ALTER TABLE if EXISTS Car RENAME COLUMN _manufacture_year TO c_manufacture_year;
ALTER TABLE if EXISTS Car RENAME COLUMN _name TO c_name;
ALTER TABLE if EXISTS Car RENAME COLUMN _description TO c_description;

ALTER TABLE if EXISTS Car ALTER COLUMN c_description TYPE TEXT;