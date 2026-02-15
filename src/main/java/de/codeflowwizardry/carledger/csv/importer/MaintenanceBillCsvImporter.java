package de.codeflowwizardry.carledger.csv.importer;

import java.math.BigInteger;
import java.util.Set;

import de.codeflowwizardry.carledger.csv.CsvColumnMapping;
import de.codeflowwizardry.carledger.csv.CsvFieldExtractors;
import de.codeflowwizardry.carledger.csv.CsvImporter;
import de.codeflowwizardry.carledger.data.factory.MaintenanceBillFactory;
import de.codeflowwizardry.carledger.rest.car.maintenance.MaintenanceBillInput;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class MaintenanceBillCsvImporter implements CsvImporter<MaintenanceBillInput>
{
	private final MaintenanceBillFactory factory;

	@Inject
	public MaintenanceBillCsvImporter(MaintenanceBillFactory factory)
	{
		this.factory = factory;
	}

	@Override
	public MaintenanceBillInput parseRow(String[] line, CsvColumnMapping mapping, BigInteger vat)
	{
		return new MaintenanceBillInput(
				CsvFieldExtractors.getBigDecimal(line, mapping, "total"),
				CsvFieldExtractors.getBigInteger(line, mapping, "vatRate", vat),
				CsvFieldExtractors.getLocalDate(line, mapping, "date"),
				CsvFieldExtractors.getBigInteger(line, mapping, "odometer"),
				CsvFieldExtractors.getBigDecimal(line, mapping, "laborCost"),
				CsvFieldExtractors.getBigDecimal(line, mapping, "partsCost"),
				CsvFieldExtractors.getString(line, mapping, "description"),
				CsvFieldExtractors.getString(line, mapping, "workshop"));
	}

	@Override
	public void save(MaintenanceBillInput entity, long carId, String name)
	{
		factory.create(entity, carId, name);
	}

	public static Set<String> getAllFields()
	{
		return Set.of("date", "vatRate", "total", "odometer", "laborCost", "partsCost", "description", "workshop");
	}

	public static Set<String> getRequiredFields()
	{
		return Set.of("date", "total", "workshop");
	}

	@Override
	public void validateMapping(CsvColumnMapping mapping)
	{
		mapping.validateRequired(getRequiredFields());
	}
}
