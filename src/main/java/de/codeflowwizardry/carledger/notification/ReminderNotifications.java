package de.codeflowwizardry.carledger.notification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.OdometerService;
import de.codeflowwizardry.carledger.ReminderService;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.ReminderEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarAccessRepository;
import de.codeflowwizardry.carledger.data.repository.ReminderRepository;
import de.codeflowwizardry.carledger.rest.car.reminder.Reminder;
import de.codeflowwizardry.carledger.rest.car.reminder.ReminderStatus;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Once a day, the owner and the editors of a car get one notification with its
 * reminders that became due soon or overdue (the status of the app: 30 days or
 * 1000 km before), not the viewers and drivers: in the inbox, by mail and push
 * if they want the reminders. A reminder is
 * notified once per due date and odometer; done or changed, it is notified
 * again
 * when its new due comes.
 */
@ApplicationScoped
public class ReminderNotifications
{
	private static final Logger LOG = LoggerFactory.getLogger(ReminderNotifications.class);

	private final AccountRepository accountRepository;
	private final ReminderRepository reminderRepository;
	private final CarAccessRepository carAccessRepository;
	private final OdometerService odometerService;
	private final Notifications notifications;

	@Inject
	public ReminderNotifications(AccountRepository accountRepository,
			ReminderRepository reminderRepository, CarAccessRepository carAccessRepository,
			OdometerService odometerService, Notifications notifications)
	{
		this.accountRepository = accountRepository;
		this.reminderRepository = reminderRepository;
		this.carAccessRepository = carAccessRepository;
		this.odometerService = odometerService;
		this.notifications = notifications;
	}

	@Scheduled(cron = "{carledger.mail.reminders-cron}", identity = "reminder-notifications")
	void sendDaily()
	{
		send(LocalDate.now());
	}

	/**
	 * The due reminders are claimed at once, then every account gets its
	 * notification in its own transaction: a failing account does not stop the
	 * others, its mail is sent after its commit
	 */
	void send(LocalDate today)
	{
		// the inbox gets them without mail and push as well
		Map<Long, List<Reminder>> perAccount = QuarkusTransaction.requiringNew().call(() -> claimDue(today));
		perAccount.forEach((accountId, due) -> {
			try
			{
				QuarkusTransaction.requiringNew()
						.run(() -> notifications.reminders(accountRepository.findById(accountId), due));
			}
			catch (Exception e)
			{
				LOG.error("Could not mail the reminders of account {}", accountId, e);
			}
		});
	}

	/**
	 * @return the due reminders per account, the most urgent first; a reminder
	 *         goes to the owner and the editors of its car
	 */
	private Map<Long, List<Reminder>> claimDue(LocalDate today)
	{
		Map<Long, List<Reminder>> perAccount = new LinkedHashMap<>();
		Map<Long, List<AccountEntity>> recipients = new HashMap<>();
		Map<Long, Integer> odometers = new HashMap<>();
		for (ReminderEntity entity : reminderRepository.find("from reminder r join fetch r.car c join fetch c.user")
				.list())
		{
			CarEntity car = entity.getCar();
			List<AccountEntity> to = recipients.computeIfAbsent(car.getId(), id -> recipients(car));
			if (to.isEmpty())
			{
				continue;
			}
			int odometer = odometers.computeIfAbsent(car.getId(),
					id -> odometerService.current(id, car.getUser().getUserId()).get(id).value());
			Reminder reminder = ReminderService.convert(entity, odometer, today);
			if (reminder.status() != ReminderStatus.UPCOMING && claim(entity))
			{
				to.forEach(account -> perAccount.computeIfAbsent(account.getId(), id -> new ArrayList<>())
						.add(reminder));
			}
		}
		perAccount.values().forEach(due -> due.sort(ReminderService.BY_URGENCY));
		return perAccount;
	}

	/**
	 * @return the owner and the editors of the car: the reminders are in their
	 *         inbox, by mail and push if they want them
	 */
	private List<AccountEntity> recipients(CarEntity car)
	{
		return Stream.concat(Stream.of(car.getUser()), carAccessRepository.editorsOf(car.getId()).stream())
				.toList();
	}

	/**
	 * Marks the reminder as mailed for its due. An update with a condition, so of
	 * several replicas running the job only one notifies it.
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
