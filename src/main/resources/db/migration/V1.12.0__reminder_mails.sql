-- Mails about due reminders: the due date and odometer a reminder was mailed for (once per due), and the opt-out of
-- the account

ALTER TABLE reminder ADD COLUMN rm_notified_for VARCHAR(40);
ALTER TABLE account ADD COLUMN notify_reminders BOOLEAN NOT NULL DEFAULT TRUE;
