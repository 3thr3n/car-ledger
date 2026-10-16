package de.codeflowwizardry.carledger.rest.records;

import de.codeflowwizardry.carledger.data.AccountEntity;

/**
 * Notifications of the own account by e-mail
 *
 * @param available
 *            whether the server sends mails at all
 * @param byMail
 *            the notifications by mail, the other channel are the browsers
 * @param email
 *            the address of the login, <code>null</code> without one: no mails
 * @param newRequests
 *            mail about new requests, only sent to administrators
 * @param requestDecided
 *            mail when an own request was decided
 * @param reminders
 *            mail the reminders of the cars once they are due soon
 */
public record NotificationSettings(boolean available, boolean byMail, String email, boolean newRequests,
		boolean requestDecided,
		boolean reminders)
{
	public static NotificationSettings convert(AccountEntity account, boolean available)
	{
		return new NotificationSettings(available, account.isNotifyByMail(), account.getEmail(),
				account.isNotifyNewRequests(),
				account.isNotifyRequestDecided(), account.isNotifyReminders());
	}
}
