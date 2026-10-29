-- The latest status a GPS tracker reported (#466): firmware, storage, reset reason, last upload, WLAN, installed
-- parts. Only the latest one, no history; replaced by every report.

ALTER TABLE device
    ADD COLUMN dv_status    JSONB,
    ADD COLUMN dv_status_at TIMESTAMP WITH TIME ZONE;
