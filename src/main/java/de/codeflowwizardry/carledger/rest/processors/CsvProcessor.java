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

import de.codeflowwizardry.carledger.data.Bill;
import de.codeflowwizardry.carledger.data.Car;
import de.codeflowwizardry.carledger.data.repository.BillRepository;
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

	private final BillRepository billRepository;

	@Inject
	public CsvProcessor(BillRepository billRepository)
	{
		this.billRepository = billRepository;
	}

	public void processCsv(File csv, CsvOrder csvOrder, Car car, boolean hasHeader)
			throws WebApplicationException
	{
		int skipLines = 0;

		if (hasHeader)
		{
			skipLines = 1;
		}

		try
		{
			processAndReadCsv(csv, csvOrder, car, skipLines);
		}
		catch (IOException e)
		{
			LOG.error("File cannot be read! Internal error!", e);
			throw new InternalServerErrorException("Something went wrong while reading the sent file!");
		}
	}

	private void processAndReadCsv(File csv, CsvOrder csvOrder, Car car, int skipLines) throws IOException
	{
		try (CSVReader csvReader = new CSVReaderBuilder(new FileReader(csv)).withSkipLines(skipLines).build())
		{
			List<Bill> billList = new ArrayList<>();

			String[] line;
			while ((line = csvReader.readNext()) != null)
			{
				Bill bill = new Bill();
				bill.setCar(car);

				bill.setUnit(parseToBigDecimal(line, csvOrder.unit()));
				bill.setEstimate(parseToBigDecimal(line, csvOrder.estimate()));
				bill.setDistance(parseToBigDecimal(line, csvOrder.distance()));
				bill.setPricePerUnit(parseToBigDecimal(line, csvOrder.pricePerUnit()));
				bill.setDay(parseToLocalDate(line, csvOrder.day()));

				if (!billRepository.isPersistent(bill))
				{
					billList.add(bill);
				}
			}

			billRepository.persist(billList);
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
