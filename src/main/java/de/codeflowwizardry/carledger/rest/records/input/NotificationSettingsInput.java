package de.codeflowwizardry.carledger.rest.records.input;

/**
 * Notifications of the own account by e-mail, unchanged without a value
 *
 * @param newRequests
 *            mail about new requests, only sent to administrators
 * @param requestDecided
 *            mail when an own request was decided
 * @param reminders
 *            mail the reminders of the cars once they are due soon
 */
public record NotificationSettingsInput(Boolean newRequests, Boolean requestDecided, Boolean reminders)
{
}
