package de.codeflowwizardry.carledger.data.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.MiscellaneousBillEntity;
import de.codeflowwizardry.carledger.data.repository.CarAccess.Action;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * For now only the owner may do anything with a car, a foreign car is never
 * found, whatever the action.
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
	EntityManager entityManager;

	long carId;
	long billId;

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

	@Test
	void shouldNotDeleteAForeignCar()
	{
		assertFalse(QuarkusTransaction.requiringNew().call(() -> carRepository.deleteForUser(carId, "paul")));
		assertTrue(QuarkusTransaction.requiringNew().call(() -> carRepository.deleteForUser(carId, "peter")));
	}
}
