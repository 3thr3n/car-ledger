package de.codeflowwizardry.carledger.notification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.OdometerService;
import de.codeflowwizardry.carledger.ReminderService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.ReminderEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.ReminderRepository;
import de.codeflowwizardry.carledger.rest.car.reminder.Reminder;
import de.codeflowwizardry.carledger.rest.car.reminder.ReminderStatus;
import de.codeflowwizardry.carledger.rest.records.Odometer;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Once a day, every account gets one mail with its reminders that became due
 * soon or overdue (the status of the app: 30 days or 1000 km before). A
 * reminder is mailed once per due date and odometer; done or changed, it is
 * mailed again when its new due comes.
 */
@ApplicationScoped
public class ReminderMails
{
	private static final Logger LOG = LoggerFactory.getLogger(ReminderMails.class);

	private final MailConfig config;
	private final AccountRepository accountRepository;
	private final ReminderRepository reminderRepository;
	private final OdometerService odometerService;
	private final MailNotifications mails;

	@Inject
	public ReminderMails(MailConfig config, AccountRepository accountRepository,
			ReminderRepository reminderRepository, OdometerService odometerService, MailNotifications mails)
	{
		this.config = config;
		this.accountRepository = accountRepository;
		this.reminderRepository = reminderRepository;
		this.odometerService = odometerService;
		this.mails = mails;
	}

	@Scheduled(cron = "{carledger.mail.reminders-cron}", identity = "reminder-mails")
	void sendDaily()
	{
		send(LocalDate.now());
	}

	/**
	 * One transaction per account: a failing account does not stop the others,
	 * its mail is sent after its commit
	 */
	void send(LocalDate today)
	{
		if (!config.enabled())
		{
			return;
		}
		List<Long> accounts = QuarkusTransaction.requiringNew().call(() -> accountRepository
				.find("notifyReminders = true and email is not null").stream()
				.map(AccountEntity::getId)
				.toList());
		for (Long id : accounts)
		{
			try
			{
				QuarkusTransaction.requiringNew().run(() -> send(id, today));
			}
			catch (Exception e)
			{
				LOG.error("Could not mail the reminders of account {}", id, e);
			}
		}
	}

	private void send(long accountId, LocalDate today)
	{
		AccountEntity account = accountRepository.findById(accountId);
		List<ReminderEntity> reminders = reminderRepository.list(CarRepository.ALL_CARS, account.getUserId());
		if (reminders.isEmpty())
		{
			return;
		}
		Map<Long, Odometer> odometers = odometerService.current(CarRepository.ALL_CARS, account.getUserId());

		List<Reminder> due = new ArrayList<>();
		for (ReminderEntity entity : reminders)
		{
			Reminder reminder = ReminderService.convert(entity,
					odometers.get(entity.getCar().getId()).value(), today);
			if (reminder.status() != ReminderStatus.UPCOMING && claim(entity))
			{
				due.add(reminder);
			}
		}
		due.sort(ReminderService.BY_URGENCY);
		mails.reminders(account, due);
	}

	/**
	 * Marks the reminder as mailed for its due. An update with a condition, so of
	 * several replicas running the job only one mails it.
	 *
	 * @return whether it was not mailed for this due yet
	 */
	private boolean claim(ReminderEntity reminder)
	{
		String key = reminder.dueKey();
		return reminderRepository.update(
				"notifiedFor = ?1 where id = ?2 and (notifiedFor is null or notifiedFor <> ?1)", key,
				reminder.getId()) == 1;
	}
}
