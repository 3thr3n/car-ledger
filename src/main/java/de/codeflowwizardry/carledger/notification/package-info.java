/**
 * The notifications by mail and push.
 * {@link de.codeflowwizardry.carledger.notification.Notifications} is called by
 * the services: it picks the recipients and renders the texts in their language
 * (the Qute templates of src/main/resources/templates) while the transaction is
 * open, then fires them as
 * {@link de.codeflowwizardry.carledger.notification.OutgoingMails} and
 * {@link de.codeflowwizardry.carledger.notification.push.OutgoingPushes}, which
 * {@link de.codeflowwizardry.carledger.notification.MailSender} and the
 * {@link de.codeflowwizardry.carledger.notification.push.PushSender} send after
 * the commit. A failed transaction sends nothing, a failing mail server never
 * fails a request.
 * <p>
 * The events: requests for more cars and their decisions, invitations to a car
 * and their answers, and the reminders that became due, which
 * {@link de.codeflowwizardry.carledger.notification.ReminderNotifications}
 * collects once a day.
 */
package de.codeflowwizardry.carledger.notification;
