package de.codeflowwizardry.carledger.csv.importer;

import java.util.Set;

import de.codeflowwizardry.carledger.csv.CsvColumnMapping;
import de.codeflowwizardry.carledger.csv.CsvFieldExtractors;
import de.codeflowwizardry.carledger.csv.CsvImporter;
import de.codeflowwizardry.carledger.data.ChargingLocation;
import de.codeflowwizardry.carledger.data.factory.ChargingBillFactory;
import de.codeflowwizardry.carledger.rest.car.charging.ChargingBillInput;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class ChargingBillCsvImporter implements CsvImporter<ChargingBillInput>
{
	private final ChargingBillFactory factory;

	@Inject
	public ChargingBillCsvImporter(ChargingBillFactory factory)
	{
		this.factory = factory;
	}

	@Override
	public ChargingBillInput parseRow(String[] line, CsvColumnMapping mapping, Integer vat)
	{
		return new ChargingBillInput(
				CsvFieldExtractors.getLocalDate(line, mapping, "date"),
				CsvFieldExtractors.getBigDecimal(line, mapping, "total"),
				CsvFieldExtractors.getInteger(line, mapping, "vatRate", vat),
				CsvFieldExtractors.getBigDecimal(line, mapping, "distance"),
				CsvFieldExtractors.getBigDecimal(line, mapping, "unit"),
				CsvFieldExtractors.getBigDecimal(line, mapping, "pricePerUnit"),
				CsvFieldExtractors.getEnum(line, mapping, "location", ChargingLocation.class),
				CsvFieldExtractors.getInteger(line, mapping, "stateOfCharge"));
	}

	@Override
	public void save(ChargingBillInput entity, long carId, String name)
	{
		factory.create(entity, carId, name);
	}

	public static Set<String> getAllFields()
	{
		return Set.of("date", "vatRate", "total", "distance", "unit", "pricePerUnit", "location", "stateOfCharge");
	}

	public static Set<String> getRequiredFields()
	{
		return Set.of("date", "unit", "pricePerUnit");
	}

	@Override
	public void validateMapping(CsvColumnMapping mapping)
	{
		mapping.validateRequired(getRequiredFields());
	}
}
