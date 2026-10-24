-- Accent color of the UI, one of the presets

ALTER TABLE account ADD COLUMN accent VARCHAR(10) NOT NULL DEFAULT 'TEAL';
