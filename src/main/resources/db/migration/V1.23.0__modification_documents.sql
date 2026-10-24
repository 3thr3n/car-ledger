-- Documents of a modification (Gutachten / ABE, Abnahme certificate, photos) as attachments with their kind. A file
-- belongs to a bill, a modification or the car itself (its picture).
ALTER TABLE attachment ADD COLUMN a_modification_id BIGINT REFERENCES modification (id) ON DELETE CASCADE;
ALTER TABLE attachment ADD COLUMN a_kind VARCHAR(20);

ALTER TABLE attachment ADD CONSTRAINT attachment_owner CHECK (a_bill_id IS NULL OR a_modification_id IS NULL);
ALTER TABLE attachment ADD CONSTRAINT attachment_kind CHECK ((a_kind IS NULL) = (a_modification_id IS NULL));

CREATE INDEX attachment_modification_idx ON attachment (a_modification_id);
