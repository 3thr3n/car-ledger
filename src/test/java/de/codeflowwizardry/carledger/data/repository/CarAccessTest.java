package de.codeflowwizardry.carledger.data.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.CarAccessEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.CarRole;
import de.codeflowwizardry.carledger.data.MiscellaneousBillEntity;
import de.codeflowwizardry.carledger.data.TripEntity;
import de.codeflowwizardry.carledger.data.TripType;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * The owner may do anything with a car, another account only what its role
 * allows, within the period of its access; a foreign car is never found.
 */
@QuarkusTest
class CarAccessTest
{
	@Inject
	AccountRepository accountRepository;

	@Inject
	CarRepository carRepository;

	@Inject
	MiscellaneousBillRepository miscellaneousBillRepository;

	@Inject
	TripRepository tripRepository;

	@Inject
	EntityManager entityManager;

	@Inject
	CarAccessRepository carAccessRepository;

	/** what each role may do, the actions missing here it may not */
	private static final Map<CarRole, Set<Action>> ALLOWED = Map.of(
			CarRole.EDITOR, EnumSet.of(Action.USE, Action.DRIVE, Action.READ, Action.WRITE),
			CarRole.VIEWER, EnumSet.of(Action.USE, Action.READ),
			CarRole.DRIVER, EnumSet.of(Action.USE, Action.DRIVE));

	/**
	 * the action of the car an entry of another account needs: USE and DRIVE
	 * never give the entries of others
	 */
	private static Action forEntry(Action action)
	{
		return switch (action)
		{
			case USE -> Action.READ;
			case DRIVE -> Action.WRITE;
			default -> action;
		};
	}

	long carId;
	long billId;
	final UUID tripId = UUID.randomUUID();

	@BeforeEach
	void setup()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			AccountEntity peter = new AccountEntity();
			peter.setMaxCars(1);
			peter.setUserId("peter");
			accountRepository.persist(peter);

			AccountEntity paul = new AccountEntity();
			paul.setMaxCars(1);
			paul.setUserId("paul");
			accountRepository.persist(paul);

			CarEntity car = new CarEntity();
			car.setUser(peter);
			car.setName("Golf");
			car.setManufactureYear(2020);
			carRepository.persist(car);
			carId = car.getId();

			BillEntity bill = new BillEntity();
			bill.setType(BillType.MISCELLANEOUS);
			bill.setCar(car);
			bill.setDate(LocalDate.of(2026, 9, 1));
			bill.setTotal(BigDecimal.TEN);
			bill.setVatRate(19);
			entityManager.persist(bill);
			entityManager.persist(new MiscellaneousBillEntity(bill));
			billId = bill.getId();

			tripRepository.persist(new TripEntity(tripId, car, peter, TripType.PRIVATE, 0, OffsetDateTime.now(),
					false, OffsetDateTime.now()));
		});
	}

	@AfterEach
	void cleanup()
	{
		QuarkusTransaction.requiringNew().run(() -> accountRepository.deleteAll());
	}

	@ParameterizedTest
	@EnumSource(Action.class)
	void shouldFindTheCarForTheOwner(Action action)
	{
		CarEntity car = QuarkusTransaction.requiringNew().call(() -> carRepository.findById(carId, "peter", action));
		assertEquals(carId, car.getId());
	}

	@ParameterizedTest
	@EnumSource(Action.class)
	void shouldNotFindAForeignCar(Action action)
	{
		assertNull(QuarkusTransaction.requiringNew().call(() -> carRepository.findById(carId, "paul", action)));
	}

	@ParameterizedTest
	@EnumSource(Action.class)
	void shouldFindTheBillOnlyForTheOwner(Action action)
	{
		QuarkusTransaction.requiringNew().run(() -> {
			assertTrue(miscellaneousBillRepository.findBill(carId, billId, "peter", action).isPresent());
			assertTrue(miscellaneousBillRepository.findBill(carId, billId, "paul", action).isEmpty());
		});
	}

	@ParameterizedTest
	@EnumSource(Action.class)
	void shouldFindTheTripOnlyForTheOwner(Action action)
	{
		QuarkusTransaction.requiringNew().run(() -> {
			assertTrue(tripRepository.find(carId, tripId, "peter", action).isPresent());
			assertTrue(tripRepository.find(carId, tripId, "paul", action).isEmpty());
		});
	}

	@Test
	void shouldListOnlyTheCarsOfTheUser()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			assertEquals(1, carRepository.listForUser("peter").size());
			assertEquals(1, carRepository.listOwned("peter").size());
			assertEquals(1, carRepository.countOwned("peter"));

			assertTrue(carRepository.listForUser("paul").isEmpty());
			assertTrue(carRepository.listOwned("paul").isEmpty());
			assertEquals(0, carRepository.countOwned("paul"));
		});
	}

	/** gives paul the role for the car */
	private void share(CarRole role, OffsetDateTime from, OffsetDateTime until)
	{
		QuarkusTransaction.requiringNew().run(() -> {
			CarAccessEntity access = new CarAccessEntity(entityManager.find(CarEntity.class, carId),
					accountRepository.find("userId", "paul").firstResult(), role,
					accountRepository.find("userId", "peter").firstResult());
			access.setValidFrom(from);
			access.setValidUntil(until);
			carAccessRepository.persist(access);
		});
	}

	@ParameterizedTest
	@EnumSource(value = CarRole.class, names = {
			"EDITOR", "VIEWER", "DRIVER"
	})
	void shouldAllowWhatTheRoleMayDo(CarRole role)
	{
		share(role, null, null);

		for (Action action : Action.values())
		{
			boolean allowed = ALLOWED.get(role).contains(action);
			// the bill and the trip are of peter
			boolean entry = ALLOWED.get(role).contains(forEntry(action));
			QuarkusTransaction.requiringNew().run(() -> {
				assertEquals(allowed, carRepository.findById(carId, "paul", action) != null, role + " " + action);
				assertEquals(entry, miscellaneousBillRepository.findBill(carId, billId, "paul", action).isPresent(),
						role + " " + action + " of a bill");
				assertEquals(entry, tripRepository.find(carId, tripId, "paul", action).isPresent(),
						role + " " + action + " of a trip");
			});
		}
	}

	@Test
	void shouldCountAnAccessOnlyInItsPeriod()
	{
		OffsetDateTime now = OffsetDateTime.now();
		share(CarRole.EDITOR, now.plusDays(1), null);
		assertNull(QuarkusTransaction.requiringNew().call(() -> carRepository.findById(carId, "paul", Action.READ)),
				"before");

		QuarkusTransaction.requiringNew().run(() -> {
			CarAccessEntity access = carAccessRepository.findAll().firstResult();
			access.setValidFrom(now.minusDays(2));
			access.setValidUntil(now.minusDays(1));
		});
		assertNull(QuarkusTransaction.requiringNew().call(() -> carRepository.findById(carId, "paul", Action.READ)),
				"after");
		assertTrue(QuarkusTransaction.requiringNew().call(() -> carAccessRepository.rolesOf("paul")).isEmpty());

		QuarkusTransaction.requiringNew().run(() -> {
			CarAccessEntity access = carAccessRepository.findAll().firstResult();
			access.setValidUntil(now.plusDays(1));
		});
		assertNotNull(QuarkusTransaction.requiringNew().call(() -> carRepository.findById(carId, "paul", Action.READ)),
				"within");
	}

	@Test
	void shouldListTheSharedCarButNotCountIt()
	{
		share(CarRole.DRIVER, null, null);

		QuarkusTransaction.requiringNew().run(() -> {
			assertEquals(List.of(carId), carRepository.listForUser("paul").stream().map(CarEntity::getId).toList());
			assertTrue(carRepository.listOwned("paul").isEmpty(), "not in the backup of paul");
			assertEquals(0, carRepository.countOwned("paul"), "not for the car limit");
			assertEquals(Map.of(carId, CarRole.DRIVER), carAccessRepository.rolesOf("paul"));
		});
	}

	@Test
	void shouldDeleteTheAccessWithTheCar()
	{
		share(CarRole.EDITOR, null, null);

		assertFalse(QuarkusTransaction.requiringNew().call(() -> carRepository.deleteForUser(carId, "paul")),
				"only the owner");
		assertTrue(QuarkusTransaction.requiringNew().call(() -> carRepository.deleteForUser(carId, "peter")));
		assertEquals(0, QuarkusTransaction.requiringNew().call(() -> carAccessRepository.count()));
	}

	@Test
	void shouldNotDeleteAForeignCar()
	{
		assertFalse(QuarkusTransaction.requiringNew().call(() -> carRepository.deleteForUser(carId, "paul")));
		assertTrue(QuarkusTransaction.requiringNew().call(() -> carRepository.deleteForUser(carId, "peter")));
	}
}
