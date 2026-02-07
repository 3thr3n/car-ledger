CREATE TABLE bill_miscellaneous
(
    bill_id       BIGINT PRIMARY KEY REFERENCES bill (id) ON DELETE CASCADE,
    m_description TEXT
);