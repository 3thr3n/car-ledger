package de.codeflowwizardry.carledger.notification;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Starts the daily job of the reminders from tests of other packages
 */
@ApplicationScoped
public class ReminderNotificationsAccess
{
	@Inject
	ReminderNotifications reminderNotifications;

	public void send(LocalDate today)
	{
		reminderNotifications.send(today);
	}
}
