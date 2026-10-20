-- Trips of the trip log (Fahrtenbuch): a trip is started and finished, then it can't be changed. The id is created by
-- the client, so a request sent again (e.g. from the offline queue) doesn't store a trip twice.

CREATE TABLE trip
(
    id                  UUID PRIMARY KEY,
    tp_car_id           BIGINT                   NOT NULL REFERENCES car (id) ON DELETE CASCADE,
    tp_driver_id        BIGINT                   NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    tp_type             VARCHAR(10)              NOT NULL,
    tp_start_odometer   INTEGER                  NOT NULL,
    tp_end_odometer     INTEGER,
    -- times of the device
    tp_started_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    tp_ended_at         TIMESTAMP WITH TIME ZONE,
    tp_destination      VARCHAR(200),
    tp_purpose          VARCHAR(500),
    tp_business_partner VARCHAR(200),
    -- set when a trip sent later from the offline queue doesn't fit the other data
    tp_review_reason    VARCHAR(30),
    -- created by the restore of a backup, which is a file anyone can edit
    tp_restored         BOOLEAN                  NOT NULL DEFAULT FALSE,
    -- when the server received the start and the finish
    tp_created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    tp_finished_at      TIMESTAMP WITH TIME ZONE,

    CONSTRAINT trip_end CHECK ((tp_end_odometer IS NULL) = (tp_ended_at IS NULL)),
    CONSTRAINT trip_odometer CHECK (tp_start_odometer >= 0 AND tp_end_odometer >= tp_start_odometer),
    CONSTRAINT trip_time CHECK (tp_ended_at >= tp_started_at)
);

-- one open trip per car
CREATE UNIQUE INDEX trip_open_idx ON trip (tp_car_id) WHERE tp_end_odometer IS NULL;
CREATE INDEX trip_car_started_idx ON trip (tp_car_id, tp_started_at);
CREATE INDEX trip_driver_idx ON trip (tp_driver_id);

-- Append-only, also for a direct UPDATE: the start never changes, a finished trip not at all. An open trip is finished
-- once, its destination, purpose and business partner can be added then, but not changed.
CREATE FUNCTION trip_append_only() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
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

CREATE TRIGGER trip_append_only
    BEFORE UPDATE
    ON trip
    FOR EACH ROW
EXECUTE FUNCTION trip_append_only();
