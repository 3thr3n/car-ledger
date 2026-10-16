package de.codeflowwizardry.carledger.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.ReminderEntity;
import de.codeflowwizardry.carledger.data.UnitSystem;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.data.repository.ReminderRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * The daily mail of the due reminders, once per due, in the language and the
 * units of the account
 */
@QuarkusTest
class ReminderNotificationsTest
{
	private static final LocalDate TODAY = LocalDate.now();

	@Inject
	MockMailbox mailbox;

	@Inject
	ReminderNotifications reminderNotifications;

	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	ReminderRepository reminderRepository;

	long tuev;

	@BeforeEach
	@Transactional
	void setup()
	{
		accountRepository.deleteAll();

		CarEntity golf = car(account("peter", "peter@example.com", null, UnitSystem.METRIC), "Golf", 100_000);
		tuev = reminder(golf, "TÜV", TODAY.plusDays(10), null);
		reminder(golf, "Service", null, 100_500);
		reminder(golf, "Vignette", TODAY.minusDays(3), null);
		// not due soon yet
		reminder(golf, "Tires", TODAY.plusDays(60), null);

		CarEntity polo = car(account("anna", "anna@example.com", "DE", UnitSystem.US), "Polo", 50_000);
		reminder(polo, "Inspektion", null, 50_800);

		AccountEntity carol = account("carol", "carol@example.com", null, UnitSystem.METRIC);
		carol.setNotifyReminders(false);
		reminder(car(carol, "Fiat", 1000), "TÜV", TODAY, null);
		reminder(car(account("dave", null, null, UnitSystem.METRIC), "Opel", 1000), "TÜV", TODAY, null);
		mailbox.clear();
	}

	@AfterEach
	@Transactional
	void cleanup()
	{
		accountRepository.deleteAll();
	}

	private AccountEntity account(String name, String email, String language, UnitSystem units)
	{
		AccountEntity account = new AccountEntity();
		account.setUserId(name);
		account.setMaxCars(1);
		account.setEmail(email);
		account.setLanguage(language);
		account.setUnitSystem(units);
		accountRepository.persist(account);
		return account;
	}

	private CarEntity car(AccountEntity owner, String name, int odometer)
	{
		CarEntity car = new CarEntity();
		car.setUser(owner);
		car.setName(name);
		car.setManufactureYear(2020);
		car.setOdometer(odometer);
		carRepository.persist(car);
		return car;
	}

	private long reminder(CarEntity car, String title, LocalDate dueDate, Integer dueOdometer)
	{
		ReminderEntity reminder = new ReminderEntity(car);
		reminder.setTitle(title);
		reminder.setDueDate(dueDate);
		reminder.setDueOdometer(dueOdometer);
		reminderRepository.persist(reminder);
		return reminder.getId();
	}

	private List<Mail> mailsTo(String address, int expected) throws InterruptedException
	{
		long until = System.nanoTime() + Duration.ofSeconds(1).toNanos();
		while (mailbox.getMailsSentTo(address).size() < expected && System.nanoTime() < until)
		{
			Thread.sleep(20);
		}
		return mailbox.getMailsSentTo(address);
	}

	private static String date(LocalDate date, Locale locale)
	{
		return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date);
	}

	@Test
	void shouldMailTheDueRemindersOnce() throws Exception
	{
		reminderNotifications.send(TODAY);

		Mail toPeter = mailsTo("peter@example.com", 1).getFirst();
		assertEquals("3 reminders are due", toPeter.getSubject());
		String text = toPeter.getText();
		assertTrue(text.contains("- Golf: TÜV\n  due on " + date(TODAY.plusDays(10), Locale.ENGLISH) + ", in 10 days"),
				text);
		assertTrue(text.contains("- Golf: Service\n  due at 100,500 km, 500 km left"), text);
		assertTrue(text.contains("- Golf: Vignette\n  overdue since " + date(TODAY.minusDays(3), Locale.ENGLISH)),
				text);
		assertTrue(!text.contains("Tires"), text);
		assertTrue(text.contains("https://car-ledger.example.com/dashboard"), text);
		// the most urgent first
		assertTrue(text.indexOf("Vignette") < text.indexOf("TÜV") && text.indexOf("TÜV") < text.indexOf("Service"),
				text);

		// not to carol (turned off) and dave (no address)
		mailsTo("anna@example.com", 1);
		assertEquals(2, mailbox.getTotalMessagesSent());

		// the next day nothing new
		mailbox.clear();
		reminderNotifications.send(TODAY.plusDays(1));
		Thread.sleep(200);
		assertEquals(0, mailbox.getTotalMessagesSent());
	}

	@Test
	void shouldMailAgainForTheNextDue() throws Exception
	{
		reminderNotifications.send(TODAY);
		mailsTo("peter@example.com", 1);
		mailbox.clear();

		// done and due again, e.g. a repeating reminder
		setDueDate(tuev, TODAY.plusDays(5));
		reminderNotifications.send(TODAY);

		Mail toPeter = mailsTo("peter@example.com", 1).getFirst();
		assertEquals("Reminder due: TÜV (Golf)", toPeter.getSubject());
		assertEquals(1, mailbox.getTotalMessagesSent());
	}

	@Test
	void shouldMailInTheLanguageAndTheUnitsOfTheAccount() throws Exception
	{
		reminderNotifications.send(TODAY);

		Mail toAnna = mailsTo("anna@example.com", 1).getFirst();
		assertEquals("Erinnerung fällig: Inspektion (Polo)", toAnna.getSubject());
		// 50,800 km are 31,566 miles, 800 km 497 miles
		assertTrue(toAnna.getText().contains("- Polo: Inspektion\n  fällig bei 31.566 mi, noch 497 mi"),
				toAnna.getText());
	}

	@Transactional
	void setDueDate(long id, LocalDate dueDate)
	{
		reminderRepository.findById(id).setDueDate(dueDate);
	}
}
