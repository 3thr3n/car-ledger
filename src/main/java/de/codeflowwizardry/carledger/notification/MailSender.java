package de.codeflowwizardry.carledger.notification;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

/**
 * Sends the mails asynchronously after the commit, with a few retries. A
 * failing mail server never fails the request of the user, the errors are only
 * logged.
 */
@ApplicationScoped
public class MailSender
{
	private static final Logger LOG = LoggerFactory.getLogger(MailSender.class);

	private final ReactiveMailer mailer;
	private final MailConfig config;

	@Inject
	public MailSender(ReactiveMailer mailer, MailConfig config)
	{
		this.mailer = mailer;
		this.config = config;
	}

	void onMails(@Observes(during = TransactionPhase.AFTER_SUCCESS) OutgoingMails outgoing)
	{
		for (Mail mail : outgoing.mails())
		{
			send(outgoing.event(), mail);
		}
	}

	private void send(String event, Mail mail)
	{
		Duration backoff = config.backoff();
		var sending = mailer.send(mail);
		if (config.retries() > 0)
		{
			sending = sending.onFailure().retry().withBackOff(backoff, backoff.multipliedBy(16))
					.atMost(config.retries());
		}
		sending.subscribe().with(
				ignored -> LOG.debug("Mail {} sent to {}", event, mail.getTo()),
				error -> LOG.warn("Mail {} to {} not sent after {} attempts: {}", event, mail.getTo(),
						config.retries() + 1, error.toString()));
	}
}
