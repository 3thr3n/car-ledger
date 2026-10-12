-- Settings of an account and data for the administration

ALTER TABLE account ADD COLUMN vat_country VARCHAR(2) NOT NULL DEFAULT 'DE';
ALTER TABLE account ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'EUR';
-- NULL: the language of the browser is used
ALTER TABLE account ADD COLUMN language VARCHAR(2);
ALTER TABLE account ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE account ADD COLUMN last_seen TIMESTAMP WITH TIME ZONE;
