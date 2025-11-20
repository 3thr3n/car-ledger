UPDATE car
SET _manufacture_year=2025
WHERE _manufacture_year IS NULL;

UPDATE car
SET _odometer=0
WHERE _odometer IS NULL;

UPDATE car
SET _name='PLEASE CHANGE'
WHERE _name IS NULL;