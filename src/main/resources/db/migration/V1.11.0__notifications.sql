-- Notifications by e-mail: the address and the role of the login, the language of the browser for the mails of
-- accounts without a language in the settings, and the opt-out per notification

ALTER TABLE account ADD COLUMN email VARCHAR(320);
ALTER TABLE account ADD COLUMN admin BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE account ADD COLUMN browser_language VARCHAR(2);
ALTER TABLE account ADD COLUMN notify_new_requests BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE account ADD COLUMN notify_request_decided BOOLEAN NOT NULL DEFAULT TRUE;
