package de.codeflowwizardry.carledger.rest.processors;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import de.codeflowwizardry.carledger.data.CarEntity;
import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.data.repository.FuelBillRepository;
import de.codeflowwizardry.carledger.rest.records.CsvOrder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class CsvProcessor
{
	private final static Logger LOG = LoggerFactory.getLogger(CsvProcessor.class);

	private final FuelBillRepository billRepository;

	@Inject
	public CsvProcessor(FuelBillRepository billRepository)
	{
		this.billRepository = billRepository;
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
			List<FuelBillEntity> billEntityList = new ArrayList<>();

			String[] line;
			while ((line = csvReader.readNext()) != null)
			{
				FuelBillEntity billEntity = new FuelBillEntity();
				billEntity.setCar(carEntity);

				billEntity.setUnit(parseToBigDecimal(line, csvOrder.unit()));
				billEntity.setEstimate(parseToBigDecimal(line, csvOrder.estimate()));
				billEntity.setDistance(parseToBigDecimal(line, csvOrder.distance()));
				billEntity.setPricePerUnit(parseToBigDecimal(line, csvOrder.pricePerUnit()));
				billEntity.setDate(parseToLocalDate(line, csvOrder.day()));

				if (!billRepository.isPersistent(billEntity))
				{
					billEntityList.add(billEntity);
				}
			}

			billRepository.persist(billEntityList);
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
		String day = line[position];

		try
		{
			return LocalDate.parse(day);
		}
		catch (DateTimeParseException ignored)
		{
			LOG.debug("Not the iso standard!");
		}

		try
		{
			return LocalDate.parse(day, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		}
		catch (DateTimeParseException ignored)
		{
			LOG.debug("Not the german pattern!");
		}

		throw new IllegalArgumentException("Could not parse date!");
	}
}
