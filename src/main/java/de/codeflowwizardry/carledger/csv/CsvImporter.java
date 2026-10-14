package de.codeflowwizardry.carledger.csv;

import de.codeflowwizardry.carledger.rest.records.input.AbstractBillInput;

public interface CsvImporter<T extends AbstractBillInput> extends ValidatableImporter
{
	/**
	 * @param units
	 *            converts the values of the file to km, liters and cent per liter
	 */
	T parseRow(String[] line, CsvColumnMapping mapping, Integer vat, ImportUnits units);

	void save(T entity, long carId, String name);
}
