-- The id a client created for a new bill (header Idempotency-Key): a request sent again, e.g. from the offline queue
-- after a lost answer, finds the bill instead of creating it twice. Empty for the bills before and the other ways
-- (import, restore).

ALTER TABLE bill ADD COLUMN b_client_id UUID;

CREATE UNIQUE INDEX bill_client_idx ON bill (b_car_id, b_client_id) WHERE b_client_id IS NOT NULL;
