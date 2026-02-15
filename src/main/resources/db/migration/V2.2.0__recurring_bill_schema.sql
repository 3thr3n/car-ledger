CREATE TABLE recurring_bill
(
    id              BIGSERIAL PRIMARY KEY,
    r_name          TEXT           NOT NULL,
    r_description   TEXT,
    r_car_id        BIGINT         NOT NULL REFERENCES Car (id),
    r_amount        NUMERIC(38, 2) NOT NULL,
    r_category      VARCHAR(50)    NOT NULL,
    r_interval      VARCHAR(50)    NOT NULL,
    r_start_date    DATE           NOT NULL,
    r_next_due_date DATE,
    r_end_date      DATE,

    UNIQUE (r_car_id, r_category, r_amount, r_start_date, r_interval)
);

CREATE TABLE recurring_bill_payment
(
    id                   BIGSERIAL PRIMARY KEY,
    rp_recurring_bill_id BIGINT         NOT NULL REFERENCES recurring_bill (id) ON DELETE CASCADE,
    rp_due_date          DATE           NOT NULL,
    rp_paid_date         DATE           NOT NULL,
    rp_amount            NUMERIC(38, 2) NOT NULL,
    rp_auto_generated    BOOLEAN        NOT NULL,

    UNIQUE (rp_recurring_bill_id, rp_due_date)
);