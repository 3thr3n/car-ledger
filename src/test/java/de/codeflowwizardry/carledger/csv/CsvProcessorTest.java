package de.codeflowwizardry.carledger.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.records.input.AbstractBillInput;

class CsvProcessorTest
{
	/**
	 * rejects the row "invalid" like a parser, every other row fails while
	 * saving
	 */
	private static class FailingImporter implements CsvImporter<AbstractBillInput>
	{
		@Override
		public void validateMapping(CsvColumnMapping mapping)
		{
		}

		@Override
		public AbstractBillInput parseRow(String[] line, CsvColumnMapping mapping, Integer vat)
		{
			if ("invalid".equals(line[0]))
			{
				throw new IllegalArgumentException("Could not parse date 'invalid'");
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
				.process(new StringReader("2024-01-01\ninvalid\n"), new CsvColumnMapping(Map.of("date", 0)), 0,
						19, 1, "peter");

		assertEquals(2, result.getErrorCount());
		// unexpected errors only point to the log, the database details stay on the
		// server
		assertEquals("Unexpected error, see the server log", result.getErrors().get(0).message());
		// the messages of the parser are written for the user
		assertEquals("Could not parse date 'invalid'", result.getErrors().get(1).message());
	}
}
