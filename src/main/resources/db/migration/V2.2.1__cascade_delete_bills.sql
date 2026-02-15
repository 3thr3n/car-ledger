ALTER TABLE bill
    DROP CONSTRAINT bill_b_car_id_fkey,
    ADD CONSTRAINT bill_b_car_id_fkey
        FOREIGN KEY (b_car_id) REFERENCES car (id) ON DELETE CASCADE;

ALTER TABLE recurring_bill
DROP CONSTRAINT recurring_bill_r_car_id_fkey,
    ADD CONSTRAINT recurring_bill_r_car_id_fkey
        FOREIGN KEY (r_car_id) REFERENCES car (id) ON DELETE CASCADE;
