-- Charges of electric cars and plug-in hybrids, like the refuels but in kWh

CREATE TABLE bill_charging
(
    bill_id           BIGINT PRIMARY KEY REFERENCES bill (id) ON DELETE CASCADE,
    -- kWh
    c_unit            NUMERIC(38, 2) NOT NULL,
    -- cent per kWh
    c_price_per_unit  NUMERIC(38, 2) NOT NULL,
    -- km since the last charge
    c_distance        NUMERIC(38, 2),
    -- kWh per 100 km
    c_avg_consumption NUMERIC(38, 2),
    c_cost_per_km     NUMERIC(38, 2),
    -- HOME or PUBLIC
    c_location        VARCHAR(20),
    -- percent after charging
    c_state_of_charge INTEGER
);
