package de.codeflowwizardry.carledger.csv;

import java.io.IOException;
import java.io.Reader;

import de.codeflowwizardry.carledger.csv.importer.ChargingBillCsvImporter;
import de.codeflowwizardry.carledger.csv.importer.FuelBillCsvImporter;
import de.codeflowwizardry.carledger.csv.importer.MaintenanceBillCsvImporter;
import de.codeflowwizardry.carledger.csv.importer.MiscellaneousBillCsvImporter;
import de.codeflowwizardry.carledger.data.BillType;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import de.codeflowwizardry.carledger.exception.CsvImportException;

@RequestScoped
public class CsvImportService
{
	private final FuelBillCsvImporter fuelBillCsvImporter;
	private final ChargingBillCsvImporter chargingBillCsvImporter;
	private final MaintenanceBillCsvImporter maintenanceBillCsvImporter;
	private final MiscellaneousBillCsvImporter miscellaneousBillCsvImporter;

	@Inject
	public CsvImportService(FuelBillCsvImporter fuelBillCsvImporter,
			ChargingBillCsvImporter chargingBillCsvImporter,
			MaintenanceBillCsvImporter maintenanceBillCsvImporter,
			MiscellaneousBillCsvImporter miscellaneousBillCsvImporter)
	{
		this.fuelBillCsvImporter = fuelBillCsvImporter;
		this.chargingBillCsvImporter = chargingBillCsvImporter;
		this.maintenanceBillCsvImporter = maintenanceBillCsvImporter;
		this.miscellaneousBillCsvImporter = miscellaneousBillCsvImporter;
	}

	public CsvImportResult importBills(Reader csv, CsvColumnMapping mapping, long carId, String name, int skipLines,
			Integer vat, BillType type) throws IOException
	{
		CsvProcessor<?> processor = switch (type)
		{
			case FUEL -> new CsvProcessor<>(fuelBillCsvImporter);
			case CHARGING -> new CsvProcessor<>(chargingBillCsvImporter);
			case MAINTENANCE -> new CsvProcessor<>(maintenanceBillCsvImporter);
			case MISCELLANEOUS -> new CsvProcessor<>(miscellaneousBillCsvImporter);
			case RECURRING -> throw new CsvImportException(notImportable(type));
		};
		return processor.process(csv, mapping, skipLines, vat, carId, name);
	}

	/**
	 * @return why the type cannot be imported, shown to the user
	 */
	public static String notImportable(BillType type)
	{
		return switch (type)
		{
			case RECURRING -> "Recurring costs cannot be imported, their payments are booked automatically!";
			default -> "The type " + type + " cannot be imported!";
		};
	}
}
