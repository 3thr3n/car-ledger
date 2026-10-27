package de.codeflowwizardry.carledger.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.records.input.AbstractBillInput;
import de.codeflowwizardry.carledger.exception.CsvImportException;

class CsvProcessorTest
{
	/**
	 * rejects the row "invalid" like a parser, the row "library" fails with an
	 * exception of a library, every other row fails while saving
	 */
	private static class FailingImporter implements CsvImporter<AbstractBillInput>
	{
		@Override
		public void validateMapping(CsvColumnMapping mapping)
		{
		}

		@Override
		public AbstractBillInput parseRow(String[] line, CsvColumnMapping mapping, Integer vat,
				ImportUnits units)
		{
			if ("invalid".equals(line[0]))
			{
				throw new CsvImportException("Could not parse date 'invalid'");
			}
			if ("library".equals(line[0]))
			{
				throw new NumberFormatException("Character l is neither a decimal digit number, nor a decimal point");
			}
			return null;
		}

		@Override
		public void save(AbstractBillInput entity, long carId, String name)
		{
			throw new IllegalStateException("ERROR: duplicate key value violates unique constraint \"bill_pkey\"");
		}
	}

	@Test
	void shouldNotExposeTheMessageOfUnexpectedErrors() throws IOException
	{
		CsvImportResult result = new CsvProcessor<>(new FailingImporter())
				.process(new StringReader("2024-01-01\ninvalid\nlibrary\n"), new CsvColumnMapping(Map.of("date", 0)), 0,
						19, ImportUnits.of(null), 1, "peter");

		assertEquals(3, result.getErrorCount());
		// unexpected errors only point to the log, the database details stay on the
		// server
		assertEquals("Unexpected error, see the server log", result.getErrors().get(0).message());
		// the messages of the parser are written for the user
		assertEquals("Could not parse date 'invalid'", result.getErrors().get(1).message());
		// an IllegalArgumentException of a library is no message of CarLedger
		assertEquals("Unexpected error, see the server log", result.getErrors().get(2).message());
	}

	@Test
	void shouldImportAtMostTheLimitOfRows() throws IOException
	{
		CsvImporter<AbstractBillInput> counting = new FailingImporter() {
			@Override
			public void save(AbstractBillInput entity, long carId, String name)
			{
			}
		};
		String csv = "date\n" + "2024-01-01\n".repeat(CsvProcessor.MAX_ROWS + 2);

		CsvImportResult result = new CsvProcessor<>(counting)
				.process(new StringReader(csv), new CsvColumnMapping(Map.of("date", 0)), 1, 19,
						ImportUnits.of(null), 1, "peter");

		assertEquals(CsvProcessor.MAX_ROWS, result.getSuccessCount());
		assertEquals(1, result.getErrorCount());
		// the header is row 1
		assertEquals(CsvProcessor.MAX_ROWS + 2, result.getErrors().getFirst().rowNumber());
	}
}
