package de.codeflowwizardry.carledger.notification;

import java.util.List;

import io.quarkus.mailer.Mail;

/**
 * Mails of one event, sent after the transaction succeeded
 */
public record OutgoingMails(String event, List<Mail> mails)
{
}
