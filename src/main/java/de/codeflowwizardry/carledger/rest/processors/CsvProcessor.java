package de.codeflowwizardry.carledger.rest.processors;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.exception.AlreadyPresentException;
import de.codeflowwizardry.carledger.rest.records.CsvOrder;
import de.codeflowwizardry.carledger.rest.records.input.FuelBillInput;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class CsvProcessor
{
	private final static Logger LOG = LoggerFactory.getLogger(CsvProcessor.class);

	private final FuelBillFactory fuelBillFactory;

	@Inject
	public CsvProcessor(FuelBillFactory fuelBillFactory)
	{
		this.fuelBillFactory = fuelBillFactory;
	}

	public void processCsv(File csv, CsvOrder csvOrder, CarEntity carEntity, boolean hasHeader)
			throws WebApplicationException
	{
		int skipLines = 0;

		if (hasHeader)
		{
			skipLines = 1;
		}

		try
		{
			processAndReadCsv(csv, csvOrder, carEntity, skipLines);
		}
		catch (IOException e)
		{
			LOG.error("File cannot be read! Internal error!", e);
			throw new InternalServerErrorException("Something went wrong while reading the sent file!");
		}
	}

	private void processAndReadCsv(File csv, CsvOrder csvOrder, CarEntity carEntity, int skipLines) throws IOException
	{
		try (CSVReader csvReader = new CSVReaderBuilder(new FileReader(csv)).withSkipLines(skipLines).build())
		{
			String[] line;
			while ((line = csvReader.readNext()) != null)
			{
				FuelBillInput input = new FuelBillInput(
						parseToLocalDate(line, csvOrder.date()),
						BigDecimal.ZERO,
						BigInteger.valueOf(19),
						parseToBigDecimal(line, csvOrder.distance()),
						parseToBigDecimal(line, csvOrder.unit()),
						parseToBigDecimal(line, csvOrder.pricePerUnit()),
						parseToBigDecimal(line, csvOrder.estimate()));

				try
				{
					fuelBillFactory.create(input, carEntity.getId(), carEntity.getUser().getUserId());
				}
				catch (AlreadyPresentException e)
				{
					LOG.debug("Already present in database! {}", input);
				}
				catch (IllegalStateException e)
				{
					LOG.error("Could not save entity: {}", input);
					throw e;
				}
			}
		}
		catch (CsvValidationException e)
		{
			LOG.error("CSV cannot be parsed!", e);
			throw new BadRequestException("CSV cannot be parsed!");
		}
	}

	private BigDecimal parseToBigDecimal(String[] line, int position)
	{
		String distance = line[position];
		return BigDecimal.valueOf(Double.parseDouble(distance));
	}

	private LocalDate parseToLocalDate(String[] line, int position)
	{
		String date = line[position];

		try
		{
			return LocalDate.parse(date);
		}
		catch (DateTimeParseException ignored)
		{
			LOG.debug("Not the iso standard!");
		}

		try
		{
			return LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		}
		catch (DateTimeParseException ignored)
		{
			LOG.debug("Not the german pattern!");
		}

		throw new IllegalArgumentException("Could not parse date!");
	}
}
