package de.codeflowwizardry.carledger.csv;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import de.codeflowwizardry.carledger.exception.AlreadyPresentException;
import de.codeflowwizardry.carledger.rest.records.input.AbstractBillInput;

public class CsvProcessor<T extends AbstractBillInput>
{
	private static final Logger LOG = LoggerFactory.getLogger(CsvProcessor.class);

	private final CsvImporter<T> importer;

	public CsvProcessor(CsvImporter<T> importer)
	{
		this.importer = importer;
	}

	public CsvImportResult process(File csv, CsvColumnMapping mapping, int skipLines, BigInteger vat, long carId,
			String name)
			throws IOException
	{
		importer.validateMapping(mapping);

		CsvImportResult result = new CsvImportResult();

		try (CSVReader csvReader = new CSVReaderBuilder(new FileReader(csv))
				.withSkipLines(skipLines)
				.build())
		{
			String[] line;
			int rowNumber = skipLines;

			while ((line = csvReader.readNext()) != null)
			{
				rowNumber++;

				try
				{
					T entity = importer.parseRow(line, mapping, vat);
					importer.save(entity, carId, name);
					result.addSuccess(rowNumber);
				}
				catch (AlreadyPresentException e)
				{
					LOG.debug("Row {} already exists", rowNumber);
					result.addSkipped(rowNumber, "Already exists");
				}
				catch (IllegalArgumentException e)
				{
					LOG.warn("Row {}: {}", rowNumber, e.getMessage());
					result.addError(rowNumber, e.getMessage());
				}
				catch (Exception e)
				{
					LOG.error("Row {}: Unexpected error", rowNumber, e);
					result.addError(rowNumber, "Unexpected error: " + e.getMessage());
				}
			}
		}
		catch (CsvValidationException e)
		{
			LOG.error("CSV validation failed", e);
			throw new IOException("CSV cannot be parsed: " + e.getMessage(), e);
		}

		return result;
	}
}
