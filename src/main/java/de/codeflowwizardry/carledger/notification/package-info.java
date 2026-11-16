/**
 * The notifications by mail and push: the services call
 * {@link de.codeflowwizardry.carledger.notification.Notifications}, which
 * renders
 * them in the transaction; the
 * {@link de.codeflowwizardry.carledger.notification.MailSender} and the
 * {@link de.codeflowwizardry.carledger.notification.push.PushSender} send them
 * after the commit. A failed transaction sends nothing, a failing mail server
 * never fails a request.
 * <p>
 * The events: requests for more cars and their decisions, invitations and their
 * answers, and due reminders
 * ({@link de.codeflowwizardry.carledger.notification.ReminderNotifications},
 * once
 * a day).
 */
package de.codeflowwizardry.carledger.notification;
