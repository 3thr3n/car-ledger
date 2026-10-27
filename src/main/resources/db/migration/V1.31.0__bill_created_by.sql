-- Who created an entry, shown on shared cars; a driver sees and changes only the own ones. The existing entries were
-- made by the owner of their car. A deleted account leaves its entries with the car, without a creator.
ALTER TABLE bill ADD COLUMN b_created_by BIGINT REFERENCES account (id) ON DELETE SET NULL;
UPDATE bill b SET b_created_by = c.c_user_id FROM car c WHERE b.b_car_id = c.id;
CREATE INDEX bill_created_by_idx ON bill (b_created_by);
