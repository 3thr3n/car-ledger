package de.codeflowwizardry.carledger.rest.records;

import de.codeflowwizardry.carledger.data.AccountEntity;

/**
 * Notifications of the own account by e-mail
 *
 * @param available
 *            whether the server sends mails at all
 * @param email
 *            the address of the login, <code>null</code> without one: no mails
 * @param newRequests
 *            mail about new requests, only sent to administrators
 * @param requestDecided
 *            mail when an own request was decided
 */
public record NotificationSettings(boolean available, String email, boolean newRequests, boolean requestDecided)
{
	public static NotificationSettings convert(AccountEntity account, boolean available)
	{
		return new NotificationSettings(available, account.getEmail(), account.isNotifyNewRequests(),
				account.isNotifyRequestDecided());
	}
}
