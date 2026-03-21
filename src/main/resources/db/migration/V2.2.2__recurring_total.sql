alter table if exists recurring_bill add column r_total NUMERIC (38, 2);

UPDATE recurring_bill
SET r_total=0
WHERE r_total IS NULL;

ALTER TABLE if EXISTS recurring_bill ALTER COLUMN r_total SET NOT NULL;