package de.codeflowwizardry.carledger.csv.importer;

import java.util.Set;

import de.codeflowwizardry.carledger.csv.CsvColumnMapping;
import de.codeflowwizardry.carledger.csv.CsvFieldExtractors;
import de.codeflowwizardry.carledger.csv.CsvImporter;
import de.codeflowwizardry.carledger.data.factory.FuelBillFactory;
import de.codeflowwizardry.carledger.rest.car.fuel.FuelBillInput;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class FuelBillCsvImporter implements CsvImporter<FuelBillInput>
{
	private final FuelBillFactory factory;

	@Inject
	public FuelBillCsvImporter(FuelBillFactory factory)
	{
		this.factory = factory;
	}

	@Override
	public FuelBillInput parseRow(String[] line, CsvColumnMapping mapping, Integer vat)
	{
		return new FuelBillInput(
				CsvFieldExtractors.getLocalDate(line, mapping, "date"),
				CsvFieldExtractors.getBigDecimal(line, mapping, "total"),
				CsvFieldExtractors.getInteger(line, mapping, "vatRate", vat),
				CsvFieldExtractors.getBigDecimal(line, mapping, "distance"),
				CsvFieldExtractors.getBigDecimal(line, mapping, "unit"),
				CsvFieldExtractors.getBigDecimal(line, mapping, "pricePerUnit"),
				CsvFieldExtractors.getBigDecimal(line, mapping, "estimateConsumption"));
	}

	@Override
	public void save(FuelBillInput entity, long carId, String name)
	{
		factory.create(entity, carId, name);
	}

	public static Set<String> getAllFields()
	{
		return Set.of("date", "vatRate", "total", "distance", "unit", "pricePerUnit", "estimateConsumption");
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
