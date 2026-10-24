-- A modification has several bills, e.g. the part, the workshop and the Abnahme. A deleted bill drops out of it.
CREATE TABLE modification_bill
(
    mb_modification_id BIGINT NOT NULL REFERENCES modification (id) ON DELETE CASCADE,
    mb_bill_id         BIGINT NOT NULL REFERENCES bill (id) ON DELETE CASCADE,
    PRIMARY KEY (mb_modification_id, mb_bill_id)
);

CREATE INDEX modification_bill_bill_idx ON modification_bill (mb_bill_id);

INSERT INTO modification_bill (mb_modification_id, mb_bill_id)
SELECT id, md_bill_id FROM modification WHERE md_bill_id IS NOT NULL;

ALTER TABLE modification DROP COLUMN md_bill_id;
