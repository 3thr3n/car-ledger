package de.codeflowwizardry.carledger.rest.processors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.data.repository.CarRepository;
import de.codeflowwizardry.carledger.rest.records.CsvOrder;
import jakarta.persistence.EntityManager;

class CsvProcessorTest
{
	@Test
	void fileFoundButIsNotACsv()
	{
		// given
		CsvProcessor csvProcessor = new CsvProcessor(null);

		URL url = Thread.currentThread().getContextClassLoader().getResource("white_with_dot.png");

		// when/then
		assertThrows(NullPointerException.class,
				() -> csvProcessor.processCsv(new File(url.toURI()), null, null, false, BigInteger.valueOf(19)));
	}

	@Test
	void shouldProcessValidCsv()
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

		CsvProcessor csvProcessor = new CsvProcessor(fuelBillFactory);

		URL url = Thread.currentThread().getContextClassLoader().getResource("csv/import_1.csv");

		// when/then
		assertDoesNotThrow(() -> csvProcessor.processCsv(new File(url.toURI()), new CsvOrder(), carEntity, false,
				BigInteger.valueOf(19)));
	}
}
