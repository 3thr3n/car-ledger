package de.codeflowwizardry.carledger.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.webhook.CarRequestCreated;
import de.codeflowwizardry.carledger.webhook.CarRequestDecided;
import de.codeflowwizardry.carledger.webhook.WebhookEvent;
import de.codeflowwizardry.carledger.webhook.WebhookMessage;
import io.quarkus.mailer.Mail;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Turns the events of the app into mails: picks the recipients and renders the
 * texts in their language while the transaction is still open, the
 * {@link MailSender} sends them after the commit.
 * <p>
 * The texts are the templates
 * src/main/resources/templates/mail/&lt;event&gt;.&lt;language&gt;.txt,
 * their first line is the subject.
 */
@ApplicationScoped
public class MailNotifications
{
	private static final Logger LOG = LoggerFactory.getLogger(MailNotifications.class);

	private final MailConfig config;
	private final AccountRepository accountRepository;
	private final Engine engine;
	private final Event<OutgoingMails> outgoing;

	@Inject
	public MailNotifications(MailConfig config, AccountRepository accountRepository, Engine engine,
			Event<OutgoingMails> outgoing)
	{
		this.config = config;
		this.accountRepository = accountRepository;
		this.engine = engine;
		this.outgoing = outgoing;
	}

	void onEvent(@Observes WebhookMessage message)
	{
		if (!config.enabled())
		{
			return;
		}
		List<Mail> mails = switch (WebhookEvent.ofType(message.type()).orElseThrow())
		{
			case CAR_REQUEST_CREATED -> carRequestCreated((CarRequestCreated) message.data());
			case CAR_REQUEST_DECIDED -> carRequestDecided((CarRequestDecided) message.data());
		};
		if (!mails.isEmpty())
		{
			outgoing.fire(new OutgoingMails(message.type(), mails));
		}
	}

	/**
	 * To the administrators who want it, not to the one who asked
	 */
	private List<Mail> carRequestCreated(CarRequestCreated request)
	{
		List<AccountEntity> admins = accountRepository.list(
				"admin = true and notifyNewRequests = true and email is not null and userId <> ?1",
				request.account());
		return admins.stream()
				.map(admin -> mail(admin, "car-request-created", Map.of(
						"account", request.account(),
						"cars", request.cars(),
						"maxCars", request.maxCars(),
						"reason", Optional.ofNullable(request.reason()).orElse(""),
						"link", link("/admin"))))
				.toList();
	}

	/**
	 * To the user who asked, if wanted
	 */
	private List<Mail> carRequestDecided(CarRequestDecided decision)
	{
		return accountRepository.find("userId", decision.account()).singleResultOptional()
				.filter(account -> account.isNotifyRequestDecided() && account.getEmail() != null)
				.map(account -> {
					Map<String, Object> data = new HashMap<>();
					data.put("approved", "APPROVED".equals(decision.state()));
					data.put("cars", decision.cars());
					data.put("grantedMaxCars", Optional.ofNullable(decision.grantedMaxCars()).orElse(0));
					data.put("comment", Optional.ofNullable(decision.comment()).orElse(""));
					data.put("link", link("/car"));
					return List.of(mail(account, "car-request-decided", data));
				})
				.orElse(List.of());
	}

	private Mail mail(AccountEntity recipient, String event, Map<String, Object> data)
	{
		String language = recipient.getMailLanguage().toLowerCase(Locale.ROOT);
		Template template = engine.getTemplate("mail/" + event + "." + language + ".txt");
		if (template == null)
		{
			LOG.warn("No mail template {} in {}, using English", event, language);
			template = engine.getTemplate("mail/" + event + ".en.txt");
		}
		Map<String, Object> all = new HashMap<>(data);
		all.put("settingsLink", link("/settings"));
		String text = template.data(all).render().strip();
		int newline = text.indexOf('\n');
		return Mail.withText(recipient.getEmail(), text.substring(0, newline).strip(),
				text.substring(newline + 1).strip() + "\n");
	}

	/**
	 * @return the URL of the page in the app, empty without carledger.mail.app-url
	 */
	private String link(String path)
	{
		return config.appUrl()
				.map(url -> url.toString().replaceAll("/+$", "") + path)
				.orElse("");
	}
}
