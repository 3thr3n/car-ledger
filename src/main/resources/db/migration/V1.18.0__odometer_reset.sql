-- An odometer reset of the trip log (Fahrtenbuch): a replaced instrument cluster or a reset odometer. From its time on
-- the odometer counts from the new reading, the readings before it no longer count. Append-only like the trips: it is
-- added by the owner, then only deleted once with a reason, the row stays. The id is created by the client.

CREATE TABLE odometer_reset
(
    id                 UUID PRIMARY KEY,
    rs_car_id          BIGINT                   NOT NULL REFERENCES car (id) ON DELETE CASCADE,
    -- time of the device
    rs_reset_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    -- the new reading, the base from then on
    rs_odometer        INTEGER                  NOT NULL,
    -- the last reading before, if it was still shown
    rs_odometer_before INTEGER,
    rs_reason          VARCHAR(500)             NOT NULL,
    -- the accounts by their name (user_id), the row stays when the account is deleted
    rs_created_by      VARCHAR(255)             NOT NULL,
    -- created by the restore of a backup, which is a file anyone can edit
    rs_restored        BOOLEAN                  NOT NULL DEFAULT FALSE,
    -- when the server received it
    rs_created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    rs_deleted_at      TIMESTAMP WITH TIME ZONE,
    rs_deleted_by      VARCHAR(255),
    rs_delete_reason   VARCHAR(500),

    CONSTRAINT odometer_reset_odometer CHECK (rs_odometer >= 0 AND rs_odometer_before >= 0),
    CONSTRAINT odometer_reset_deleted CHECK (
        (rs_deleted_at IS NULL) = (rs_deleted_by IS NULL) AND (rs_deleted_at IS NULL) = (rs_delete_reason IS NULL))
);

-- two resets of a car at the same time can't be ordered
CREATE UNIQUE INDEX odometer_reset_car_idx ON odometer_reset (rs_car_id, rs_reset_at) WHERE rs_deleted_at IS NULL;

-- Also for a direct UPDATE: only the deletion is set, once, nothing else with it.
CREATE FUNCTION odometer_reset_append_only() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF OLD.rs_deleted_at IS NOT NULL THEN
        RAISE EXCEPTION 'Odometer reset %: a deleted reset cannot be changed', OLD.id;
    END IF;
    IF NEW.rs_deleted_at IS NULL
        OR (NEW.id, NEW.rs_car_id, NEW.rs_reset_at, NEW.rs_odometer, NEW.rs_odometer_before, NEW.rs_reason,
            NEW.rs_created_by, NEW.rs_restored, NEW.rs_created_at)
            IS DISTINCT FROM
           (OLD.id, OLD.rs_car_id, OLD.rs_reset_at, OLD.rs_odometer, OLD.rs_odometer_before, OLD.rs_reason,
            OLD.rs_created_by, OLD.rs_restored, OLD.rs_created_at) THEN
        RAISE EXCEPTION 'Odometer reset %: a reset can only be deleted', OLD.id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER odometer_reset_append_only
    BEFORE UPDATE
    ON odometer_reset
    FOR EACH ROW
EXECUTE FUNCTION odometer_reset_append_only();
