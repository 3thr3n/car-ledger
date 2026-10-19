-- A trip is deleted with a reason and stays (Fahrtenbuch): it is listed as deleted and no longer counts for the
-- odometer. Deleting sets these columns once, nothing else.

ALTER TABLE trip ADD COLUMN tp_deleted_at TIMESTAMP WITH TIME ZONE;
-- the account that deleted it by its name (user_id), the row stays when that account is deleted
ALTER TABLE trip ADD COLUMN tp_deleted_by VARCHAR(255);
ALTER TABLE trip ADD COLUMN tp_delete_reason VARCHAR(500);
ALTER TABLE trip ADD CONSTRAINT trip_deleted CHECK (
    (tp_deleted_at IS NULL) = (tp_deleted_by IS NULL) AND (tp_deleted_at IS NULL) = (tp_delete_reason IS NULL));

-- a deleted open trip doesn't block the next one
DROP INDEX trip_open_idx;
CREATE UNIQUE INDEX trip_open_idx ON trip (tp_car_id) WHERE tp_end_odometer IS NULL AND tp_deleted_at IS NULL;

-- Append-only as before (V1.14.0), and a trip is deleted once: then it can't be changed at all.
CREATE OR REPLACE FUNCTION trip_append_only() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    IF OLD.tp_deleted_at IS NOT NULL THEN
        RAISE EXCEPTION 'Trip %: a deleted trip cannot be changed', OLD.id;
    END IF;
    IF NEW.tp_deleted_at IS NOT NULL THEN
        IF (NEW.id, NEW.tp_car_id, NEW.tp_driver_id, NEW.tp_type, NEW.tp_start_odometer, NEW.tp_end_odometer,
            NEW.tp_started_at, NEW.tp_ended_at, NEW.tp_destination, NEW.tp_purpose, NEW.tp_business_partner,
            NEW.tp_review_reason, NEW.tp_restored, NEW.tp_created_at, NEW.tp_finished_at)
            IS DISTINCT FROM
           (OLD.id, OLD.tp_car_id, OLD.tp_driver_id, OLD.tp_type, OLD.tp_start_odometer, OLD.tp_end_odometer,
            OLD.tp_started_at, OLD.tp_ended_at, OLD.tp_destination, OLD.tp_purpose, OLD.tp_business_partner,
            OLD.tp_review_reason, OLD.tp_restored, OLD.tp_created_at, OLD.tp_finished_at) THEN
            RAISE EXCEPTION 'Trip %: deleting a trip changes nothing else', OLD.id;
        END IF;
        RETURN NEW;
    END IF;
    IF OLD.tp_end_odometer IS NOT NULL THEN
        RAISE EXCEPTION 'Trip %: a finished trip cannot be changed', OLD.id;
    END IF;
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.tp_car_id IS DISTINCT FROM OLD.tp_car_id
        OR NEW.tp_driver_id IS DISTINCT FROM OLD.tp_driver_id
        OR NEW.tp_type IS DISTINCT FROM OLD.tp_type
        OR NEW.tp_start_odometer IS DISTINCT FROM OLD.tp_start_odometer
        OR NEW.tp_started_at IS DISTINCT FROM OLD.tp_started_at
        OR NEW.tp_restored IS DISTINCT FROM OLD.tp_restored
        OR NEW.tp_created_at IS DISTINCT FROM OLD.tp_created_at THEN
        RAISE EXCEPTION 'Trip %: the start of a trip cannot be changed', OLD.id;
    END IF;
    IF (OLD.tp_destination IS NOT NULL AND NEW.tp_destination IS DISTINCT FROM OLD.tp_destination)
        OR (OLD.tp_purpose IS NOT NULL AND NEW.tp_purpose IS DISTINCT FROM OLD.tp_purpose)
        OR (OLD.tp_business_partner IS NOT NULL AND NEW.tp_business_partner IS DISTINCT FROM OLD.tp_business_partner)
        OR (OLD.tp_review_reason IS NOT NULL AND NEW.tp_review_reason IS DISTINCT FROM OLD.tp_review_reason) THEN
        RAISE EXCEPTION 'Trip %: the details of a trip can only be added', OLD.id;
    END IF;
    RETURN NEW;
END;
$$;
