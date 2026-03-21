package de.codeflowwizardry.carledger.csv.importer;

import java.math.BigInteger;
import java.util.Set;

import de.codeflowwizardry.carledger.csv.CsvColumnMapping;
import de.codeflowwizardry.carledger.csv.CsvFieldExtractors;
import de.codeflowwizardry.carledger.csv.CsvImporter;
import de.codeflowwizardry.carledger.data.factory.MiscellaneousBillFactory;
import de.codeflowwizardry.carledger.rest.car.miscellaneous.MiscellaneousBillInput;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class MiscellaneousBillCsvImporter implements CsvImporter<MiscellaneousBillInput>
{
	private final MiscellaneousBillFactory factory;

	@Inject
	public MiscellaneousBillCsvImporter(MiscellaneousBillFactory factory)
	{
		this.factory = factory;
	}

	@Override
	public MiscellaneousBillInput parseRow(String[] line, CsvColumnMapping mapping, BigInteger vat)
	{
		return new MiscellaneousBillInput(
				CsvFieldExtractors.getBigDecimal(line, mapping, "total"),
				CsvFieldExtractors.getBigInteger(line, mapping, "vatRate", vat),
				CsvFieldExtractors.getLocalDate(line, mapping, "date"),
				CsvFieldExtractors.getString(line, mapping, "description"));
	}

	@Override
	public void save(MiscellaneousBillInput entity, long carId, String name)
	{
		factory.create(entity, carId, name);
	}

	public static Set<String> getAllFields()
	{
		return Set.of("date", "vatRate", "total", "description");
	}

	public static Set<String> getRequiredFields()
	{
		return Set.of("total", "date");
	}

	@Override
	public void validateMapping(CsvColumnMapping mapping)
	{
		mapping.validateRequired(getRequiredFields());
	}
}
