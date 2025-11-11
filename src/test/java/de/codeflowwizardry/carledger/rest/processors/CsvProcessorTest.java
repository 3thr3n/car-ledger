package de.codeflowwizardry.carledger.rest.processors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import de.codeflowwizardry.carledger.data.BillEntity;
import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.data.repository.BillRepository;
import de.codeflowwizardry.carledger.rest.records.CsvOrder;

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
				() -> csvProcessor.processCsv(new File(url.toURI()), null, null, false));
	}

	@Test
	void shouldProcessValidCsv() throws URISyntaxException
	{
		// given
		BillRepository billRepository = new BillRepository() {
			@Override
			public boolean isPersistent(BillEntity entity)
			{
				return false;
			}

			@Override
			public void persist(Iterable<BillEntity> entities)
			{
				// Do nothing
			}
		};

		CsvProcessor csvProcessor = new CsvProcessor(billRepository);

		URL url = Thread.currentThread().getContextClassLoader().getResource("csv/import_1.csv");

		// when/then
		assertDoesNotThrow(() -> csvProcessor.processCsv(new File(url.toURI()), new CsvOrder(), null, false));
	}
}
