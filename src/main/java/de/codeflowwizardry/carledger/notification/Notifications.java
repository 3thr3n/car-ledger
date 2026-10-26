package de.codeflowwizardry.carledger.notification;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.UnitSystem;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.notification.push.OutgoingPushes;
import de.codeflowwizardry.carledger.notification.push.PushConfig;
import de.codeflowwizardry.carledger.notification.push.PushMessage;
import de.codeflowwizardry.carledger.notification.push.PushSubscriptionEntity;
import de.codeflowwizardry.carledger.notification.push.PushSubscriptionRepository;
import de.codeflowwizardry.carledger.rest.car.reminder.Reminder;
import de.codeflowwizardry.carledger.webhook.CarAccessEvent;
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
 * Turns the events of the app into notifications: picks the recipients and
 * renders the texts in their language while the transaction is still open,
 * the {@link MailSender} and the
 * {@link de.codeflowwizardry.carledger.notification.push.PushSender} send them
 * after the commit. Every recipient gets a mail (if wanted and with an
 * address) and a push message on every browser that subscribed.
 * <p>
 * The texts are the templates
 * src/main/resources/templates/{mail,push}/&lt;event&gt;.&lt;language&gt;.txt,
 * their first line is the subject or the title.
 */
@ApplicationScoped
public class Notifications
{
	private static final Logger LOG = LoggerFactory.getLogger(Notifications.class);
	private static final double KM_PER_MILE = 1.609344;

	private final MailConfig mailConfig;
	private final PushConfig pushConfig;
	private final AccountRepository accountRepository;
	private final PushSubscriptionRepository subscriptions;
	private final Engine engine;
	private final ObjectMapper mapper;
	private final Event<OutgoingMails> outgoingMails;
	private final Event<OutgoingPushes> outgoingPushes;

	@Inject
	public Notifications(MailConfig mailConfig, PushConfig pushConfig, AccountRepository accountRepository,
			PushSubscriptionRepository subscriptions, Engine engine, ObjectMapper mapper,
			Event<OutgoingMails> outgoingMails, Event<OutgoingPushes> outgoingPushes)
	{
		this.mailConfig = mailConfig;
		this.pushConfig = pushConfig;
		this.accountRepository = accountRepository;
		this.subscriptions = subscriptions;
		this.engine = engine;
		this.mapper = mapper;
		this.outgoingMails = outgoingMails;
		this.outgoingPushes = outgoingPushes;
	}

	/**
	 * @return whether the account gets notifications at all: by mail or on a
	 *         browser
	 */
	public boolean canReach(AccountEntity account)
	{
		return wantsMail(account) || (pushConfig.enabled() && subscriptions.hasAny(account));
	}

	private boolean wantsMail(AccountEntity account)
	{
		return mailConfig.enabled() && account.isNotifyByMail() && account.getEmail() != null;
	}

	void onEvent(@Observes WebhookMessage message)
	{
		if (!mailConfig.enabled() && !pushConfig.enabled())
		{
			return;
		}
		Batch batch = new Batch(message.type());
		switch (WebhookEvent.ofType(message.type()).orElseThrow())
		{
			case CAR_REQUEST_CREATED -> carRequestCreated(batch, (CarRequestCreated) message.data());
			case CAR_REQUEST_DECIDED -> carRequestDecided(batch, (CarRequestDecided) message.data());
			case CAR_ACCESS_INVITED -> carAccessInvited(batch, (CarAccessEvent) message.data());
			case CAR_ACCESS_ANSWERED -> carAccessAnswered(batch, (CarAccessEvent) message.data());
			// the owner did it, the end of an access follows with its own notification
			case CAR_ACCESS_CHANGED, CAR_ACCESS_REVOKED -> {
			}
		}
		batch.fire();
	}

	/**
	 * To the administrators who want it, not to the one who asked
	 */
	private void carRequestCreated(Batch batch, CarRequestCreated request)
	{
		List<AccountEntity> admins = accountRepository.list(
				"admin = true and notifyNewRequests = true and userId <> ?1", request.account());
		for (AccountEntity admin : admins)
		{
			batch.add(admin, "car-request-created", "/admin", "car-request-" + request.requestId(), Map.of(
					"account", request.account(),
					"cars", request.cars(),
					"maxCars", request.maxCars(),
					"reason", Optional.ofNullable(request.reason()).orElse("")));
		}
	}

	/**
	 * To the user who asked, if wanted
	 */
	private void carRequestDecided(Batch batch, CarRequestDecided decision)
	{
		accountRepository.find("userId", decision.account()).singleResultOptional()
				.filter(AccountEntity::isNotifyRequestDecided)
				.ifPresent(account -> {
					Map<String, Object> data = new HashMap<>();
					data.put("approved", "APPROVED".equals(decision.state()));
					data.put("cars", decision.cars());
					data.put("grantedMaxCars", Optional.ofNullable(decision.grantedMaxCars()).orElse(0));
					data.put("comment", Optional.ofNullable(decision.comment()).orElse(""));
					batch.add(account, "car-request-decided", "/car", "car-request-" + decision.requestId(), data);
				});
	}

	/**
	 * To the invited account, once it exists: one invited by an address it
	 * doesn't have yet sees the invitation after its first login
	 */
	private void carAccessInvited(Batch batch, CarAccessEvent invitation)
	{
		if (invitation.account() == null)
		{
			return;
		}
		accountRepository.find("userId", invitation.account()).singleResultOptional().ifPresent(account -> {
			DateTimeFormatter dates = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
					.withLocale(Locale.forLanguageTag(account.getMailLanguage().toLowerCase(Locale.ROOT)));
			Map<String, Object> data = new HashMap<>();
			data.put("owner", invitation.owner());
			data.put("car", invitation.car());
			data.put("role", invitation.role());
			data.put("from", invitation.validFrom() == null ? "" : dates.format(day(invitation.validFrom())));
			// the last day of the access, the end is exclusive
			data.put("until",
					invitation.validUntil() == null ? "" : dates.format(day(invitation.validUntil().minusNanos(1))));
			batch.add(account, "car-access-invited", "/car", "car-access-" + invitation.accessId(), data);
		});
	}

	/**
	 * To the owner of the car
	 */
	private void carAccessAnswered(Batch batch, CarAccessEvent answer)
	{
		accountRepository.find("userId", answer.owner()).singleResultOptional().ifPresent(owner -> batch.add(owner,
				"car-access-answered", "/car/" + answer.carId() + "/edit", "car-access-" + answer.accessId(),
				Map.of("account", answer.by(), "car", answer.car(), "accepted", "ACCEPTED".equals(answer.state()))));
	}

	/**
	 * @return the day of the time on the server, the app shows the days of the
	 *         access
	 */
	private static LocalDate day(OffsetDateTime time)
	{
		return time.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
	}

	/**
	 * The reminders that became due to the owner of the cars, in one mail and one
	 * push message. Sent after the commit.
	 */
	public void reminders(AccountEntity owner, List<Reminder> due)
	{
		if (due.isEmpty())
		{
			return;
		}
		Locale locale = Locale.forLanguageTag(owner.getMailLanguage().toLowerCase(Locale.ROOT));
		boolean miles = owner.getUnitSystem() != UnitSystem.METRIC;
		DateTimeFormatter dates = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);
		NumberFormat numbers = NumberFormat.getIntegerInstance(locale);

		List<Map<String, Object>> items = due.stream().map(reminder -> {
			Map<String, Object> item = new HashMap<>();
			item.put("car", reminder.carName());
			item.put("title", reminder.title());
			item.put("note", Optional.ofNullable(reminder.note()).orElse(""));
			item.put("date", reminder.dueDate() == null ? "" : dates.format(reminder.dueDate()));
			item.put("daysLeft", reminder.daysLeft() == null ? 0 : reminder.daysLeft());
			item.put("odometer",
					reminder.dueOdometer() == null ? "" : numbers.format(distance(reminder.dueOdometer(), miles)));
			int left = reminder.kmLeft() == null ? 0 : distance(reminder.kmLeft(), miles);
			item.put("distanceLeft", left);
			item.put("distanceLeftText", numbers.format(left));
			return item;
		}).toList();

		Map<String, Object> data = new HashMap<>();
		data.put("reminders", items);
		data.put("first", items.getFirst());
		data.put("count", items.size());
		data.put("unit", miles ? "mi" : "km");
		Batch batch = new Batch("reminders.due");
		batch.add(owner, "reminders-due", "/dashboard", "reminders", data);
		batch.fire();
	}

	/**
	 * A push message to every browser of the account, to try the subscription
	 *
	 * @return the number of browsers
	 */
	public int pushTest(AccountEntity account)
	{
		Batch batch = new Batch("push.test");
		int browsers = batch.addPushes(account, "push-test", "/settings", "push-test", Map.of());
		batch.fire();
		return browsers;
	}

	/**
	 * @return the km in the units of the account, whole miles or km
	 */
	private static int distance(int km, boolean miles)
	{
		return miles ? (int) Math.round(km / KM_PER_MILE) : km;
	}

	/**
	 * @return the URL of the page in the app, empty without carledger.mail.app-url
	 */
	private String link(String path)
	{
		return mailConfig.appUrl()
				.map(url -> url.toString().replaceAll("/+$", "") + path)
				.orElse("");
	}

	/**
	 * @return the first line and the rest of the template in the language of the
	 *         account, English without one
	 */
	private String[] render(String channel, AccountEntity recipient, String event, Map<String, Object> data)
	{
		String language = recipient.getMailLanguage().toLowerCase(Locale.ROOT);
		Template template = engine.getTemplate(channel + "/" + event + "." + language + ".txt");
		if (template == null)
		{
			LOG.warn("No {} template {} in {}, using English", channel, event, language);
			template = engine.getTemplate(channel + "/" + event + ".en.txt");
		}
		String text = template.data(data).render().strip();
		int newline = text.indexOf('\n');
		return newline < 0
				? new String[] {
						text, ""
				}
				: new String[] {
						text.substring(0, newline).strip(), text.substring(newline + 1).strip()
				};
	}

	/**
	 * The notifications of one event, fired at once
	 */
	private class Batch
	{
		private final String event;
		private final List<Mail> mails = new ArrayList<>();
		private final List<PushMessage> pushes = new ArrayList<>();

		Batch(String event)
		{
			this.event = event;
		}

		/**
		 * @param path
		 *            the page of the app the notification opens
		 * @param tag
		 *            a newer push message with the same tag replaces the older one
		 */
		void add(AccountEntity recipient, String template, String path, String tag, Map<String, Object> data)
		{
			if (wantsMail(recipient))
			{
				Map<String, Object> all = new HashMap<>(data);
				all.put("link", link(path));
				all.put("settingsLink", link("/settings"));
				String[] text = render("mail", recipient, template, all);
				mails.add(Mail.withText(recipient.getEmail(), text[0], text[1] + "\n"));
			}
			addPushes(recipient, template, path, tag, data);
		}

		int addPushes(AccountEntity recipient, String template, String path, String tag, Map<String, Object> data)
		{
			if (!pushConfig.enabled())
			{
				return 0;
			}
			List<PushSubscriptionEntity> browsers = subscriptions.listFor(recipient);
			if (browsers.isEmpty())
			{
				return 0;
			}
			String[] text = render("push", recipient, template, data);
			Map<String, String> payload = new LinkedHashMap<>();
			payload.put("title", text[0]);
			payload.put("body", text[1]);
			payload.put("url", path);
			payload.put("tag", tag);
			String json;
			try
			{
				json = mapper.writeValueAsString(payload);
			}
			catch (JsonProcessingException e)
			{
				throw new IllegalStateException("The push message cannot be written", e);
			}
			for (PushSubscriptionEntity browser : browsers)
			{
				pushes.add(new PushMessage(browser.getId(), browser.getEndpoint(), browser.getP256dh(),
						browser.getAuth(), json));
			}
			return browsers.size();
		}

		void fire()
		{
			if (!mails.isEmpty())
			{
				outgoingMails.fire(new OutgoingMails(event, mails));
			}
			if (!pushes.isEmpty())
			{
				outgoingPushes.fire(new OutgoingPushes(event, pushes));
			}
		}
	}
}
