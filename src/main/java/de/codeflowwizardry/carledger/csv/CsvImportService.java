package de.codeflowwizardry.carledger.csv;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import de.codeflowwizardry.carledger.csv.importer.FuelBillCsvImporter;
import de.codeflowwizardry.carledger.csv.importer.MaintenanceBillCsvImporter;
import de.codeflowwizardry.carledger.csv.importer.MiscellaneousBillCsvImporter;
import de.codeflowwizardry.carledger.data.BillType;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class CsvImportService
{
	private final FuelBillCsvImporter fuelBillCsvImporter;
	private final MaintenanceBillCsvImporter maintenanceBillCsvImporter;
	private final MiscellaneousBillCsvImporter miscellaneousBillCsvImporter;

	@Inject
	public CsvImportService(FuelBillCsvImporter fuelBillCsvImporter,
			MaintenanceBillCsvImporter maintenanceBillCsvImporter,
			MiscellaneousBillCsvImporter miscellaneousBillCsvImporter)
	{
		this.fuelBillCsvImporter = fuelBillCsvImporter;
		this.maintenanceBillCsvImporter = maintenanceBillCsvImporter;
		this.miscellaneousBillCsvImporter = miscellaneousBillCsvImporter;
	}

	public CsvImportResult importBills(File csv, CsvColumnMapping mapping, long carId, String name, int skipLines,
			BigInteger vat, BillType type) throws IOException
	{
		CsvProcessor<?> processor = null;

		switch (type)
		{
			case FUEL -> processor = new CsvProcessor<>(fuelBillCsvImporter);
			case MAINTENANCE -> processor = new CsvProcessor<>(maintenanceBillCsvImporter);
			case MISCELLANEOUS -> processor = new CsvProcessor<>(miscellaneousBillCsvImporter);
		}
		if (processor == null)
		{
			throw new IllegalArgumentException("Type does not exist!");
		}
		return processor.process(csv, mapping, skipLines, vat, carId, name);
	}
}
