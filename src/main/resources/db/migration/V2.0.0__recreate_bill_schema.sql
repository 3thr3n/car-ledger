-- V2.0.0__recreate_bill_schema.sql

-- 1. Drop old table
DROP TABLE IF EXISTS Bill CASCADE;

-- 2. Create new base bill table
CREATE TABLE bill
(
    id           BIGSERIAL PRIMARY KEY,
    b_type       VARCHAR(50)    NOT NULL,
    b_date       DATE           NOT NULL,
    b_car_id     BIGINT         NOT NULL,
    b_total      NUMERIC(38, 2) NOT NULL,
    b_vat_rate   BIGINT         NOT NULL,
    b_net_amount NUMERIC(38, 2) NOT NULL,
    b_ust_amount NUMERIC(38, 2) NOT NULL,
);

-- 3. Create fuel-specific table
CREATE TABLE bill_fuel
(
    bill_id           BIGINT PRIMARY KEY REFERENCES bill (id) ON DELETE CASCADE,

    f_distance        NUMERIC(38, 2),
    f_estimate        NUMERIC(38, 2),

    f_unit            NUMERIC(38, 3),
    f_price_per_unit  NUMERIC(38, 4),

    f_avg_consumption NUMERIC(38, 2),
    f_cost_per_km     NUMERIC(38, 4)
);

-- 4. Create maintenance-specific table
CREATE TABLE bill_maintenance
(
    bill_id       BIGINT PRIMARY KEY REFERENCES bill (id) ON DELETE CASCADE,

    m_odometer    BIGINT,
    m_description TEXT,
    m_workshop    TEXT,
    m_labor_cost  NUMERIC(38, 2),
    m_parts_cost  NUMERIC(38, 2)
);