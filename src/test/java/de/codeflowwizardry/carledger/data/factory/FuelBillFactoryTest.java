package de.codeflowwizardry.carledger.data.factory;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.car.fuel.FuelBillInput;
import de.codeflowwizardry.carledger.rest.processors.TestEntityManager;
import jakarta.persistence.EntityManager;

class FuelBillFactoryTest
{
	@Test
	void shouldFailValidating()
	{
		// given
		FuelBillFactory factory = new FuelBillFactory(null, null);

		// when date not set
		FuelBillInput missingDate = new FuelBillInput(null, null, null, null, null, null, null);
		assertThrows(IllegalArgumentException.class, () -> factory.validate(missingDate),
				"Date cannot be null!");

		FuelBillInput missingVat = new FuelBillInput(LocalDate.now(), null, null, null, null, null, null);
		assertThrows(IllegalArgumentException.class, () -> factory.validate(missingVat),
				"Vat rate cannot be null!");

		FuelBillInput missingKeyValues = new FuelBillInput(LocalDate.now(), null, BigInteger.TEN, null, null,
				BigDecimal.valueOf(199.9), null);
		assertThrows(IllegalArgumentException.class, () -> factory.validate(missingKeyValues),
				"At least two of 'unit', 'total' or 'pricePerUnit' must be set!");
	}

	@Test
	void shouldValidate()
	{
		// given
		FuelBillFactory factory = new FuelBillFactory(null, null);

		// when date not set
		FuelBillInput minimalValidation = new FuelBillInput(LocalDate.now(),
				BigDecimal.TEN,
				BigInteger.TEN,
				null,
				null,
				BigDecimal.valueOf(199.9),
				null);
		assertDoesNotThrow(() -> factory.validate(minimalValidation));
	}

	@Test
	void shouldCalculateUnit()
	{
		// given
		FuelBillInput missingUnit = new FuelBillInput(LocalDate.now(),
				BigDecimal.valueOf(68.78),
				BigInteger.valueOf(19),
				null,
				null,
				BigDecimal.valueOf(181.9),
				null);
		// when
		BigDecimal calculatedUnit = FuelBillFactory.calculateUnit(missingUnit, BigDecimal.valueOf(1.19));

		// then
		assertEquals(BigDecimal.valueOf(3781, 2), calculatedUnit);
	}

	@Test
	void shouldCalculateCostPerKm()
	{
		// given
		EntityManager entityManager = new TestEntityManager();

		AccountEntity accountEntity = Mockito.mock(AccountEntity.class);
		CarEntity carEntity = Mockito.mock(CarEntity.class);
		Mockito.when(carEntity.getId()).thenReturn(1L);
		Mockito.when(carEntity.getUser()).thenReturn(accountEntity);
		Mockito.when(accountEntity.getUserId()).thenReturn("bob");

		CarRepository carRepository = new CarRepository() {
			@Override
			public CarEntity findById(Long id, String user)
			{
				return carEntity;
			}
		};

		FuelBillFactory fuelBillFactory = new FuelBillFactory(entityManager, carRepository);

		FuelBillInput missingUnit = new FuelBillInput(LocalDate.now(),
				BigDecimal.valueOf(80),
				BigInteger.valueOf(19),
				BigDecimal.valueOf(400),
				null,
				BigDecimal.valueOf(181.9),
				null);
		// when
		FuelBillEntity calculatedUnit = fuelBillFactory.create(missingUnit, carEntity.getId(),
				accountEntity.getUserId());

		// then
		assertEquals(BigDecimal.valueOf(20).setScale(2, RoundingMode.HALF_UP), calculatedUnit.getCostPerKm());

	}

	@Test
	void shouldNotCalculateConsumptionOrCostPerKm()
	{
		// given
		EntityManager entityManager = new TestEntityManager();

		AccountEntity accountEntity = Mockito.mock(AccountEntity.class);
		CarEntity carEntity = Mockito.mock(CarEntity.class);
		Mockito.when(carEntity.getId()).thenReturn(1L);
		Mockito.when(carEntity.getUser()).thenReturn(accountEntity);
		Mockito.when(accountEntity.getUserId()).thenReturn("bob");

		CarRepository carRepository = new CarRepository() {
			@Override
			public CarEntity findById(Long id, String user)
			{
				return carEntity;
			}
		};

		FuelBillFactory fuelBillFactory = new FuelBillFactory(entityManager, carRepository);

		FuelBillInput noDistance = new FuelBillInput(LocalDate.now(),
				BigDecimal.valueOf(80),
				BigInteger.valueOf(19),
				null,
				null,
				BigDecimal.valueOf(181.9),
				null);
		// when
		FuelBillEntity calculatedUnit = fuelBillFactory.create(noDistance, carEntity.getId(),
				accountEntity.getUserId());

		// then
		assertEquals(BigDecimal.valueOf(0), calculatedUnit.getCostPerKm());
		assertEquals(BigDecimal.valueOf(0), calculatedUnit.getAvgConsumption());
	}
}
