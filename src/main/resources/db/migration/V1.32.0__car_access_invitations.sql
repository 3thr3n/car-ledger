-- An access starts as an invitation: it counts only once the invited account accepted it. The ones given before stay
-- accepted. A declined one stays for the owner to see, a new invitation asks again.
ALTER TABLE car_access
    ADD COLUMN ca_state VARCHAR(10) NOT NULL DEFAULT 'ACCEPTED',
    ADD CONSTRAINT car_access_state CHECK (ca_state IN ('PENDING', 'ACCEPTED', 'DECLINED'));
ALTER TABLE car_access
    ALTER COLUMN ca_state SET DEFAULT 'PENDING';

-- An account that doesn't exist yet is invited by its user name or e-mail address (in lower case), it gets the
-- invitation with its first login
ALTER TABLE car_access
    ALTER COLUMN ca_account_id DROP NOT NULL,
    ADD COLUMN ca_invitee VARCHAR(320),
    ADD CONSTRAINT car_access_invited CHECK (ca_account_id IS NOT NULL OR ca_invitee IS NOT NULL);

CREATE UNIQUE INDEX car_access_invitee ON car_access (ca_car_id, ca_invitee) WHERE ca_account_id IS NULL;
CREATE INDEX car_access_invitee_idx ON car_access (ca_invitee) WHERE ca_account_id IS NULL;

-- An invitation to an e-mail address finds only an account whose identity provider confirmed it, set with the next login
ALTER TABLE account
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
