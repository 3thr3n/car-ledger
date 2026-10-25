-- Files of the recurring costs, e.g. the contract of a leasing or the policy of an insurance, deleted with them
ALTER TABLE attachment ADD COLUMN a_recurring_bill_id BIGINT REFERENCES recurring_bill (id) ON DELETE CASCADE;
CREATE INDEX attachment_recurring_bill_idx ON attachment (a_recurring_bill_id);
ALTER TABLE attachment DROP CONSTRAINT attachment_owner;
ALTER TABLE attachment ADD CONSTRAINT attachment_owner
    CHECK (num_nonnulls(a_bill_id, a_modification_id, a_technical_inspection_id, a_recurring_bill_id) <= 1);
