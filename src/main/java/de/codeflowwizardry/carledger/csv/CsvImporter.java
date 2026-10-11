package de.codeflowwizardry.carledger.csv;

import de.codeflowwizardry.carledger.rest.records.input.AbstractBillInput;

public interface CsvImporter<T extends AbstractBillInput> extends ValidatableImporter
{
	T parseRow(String[] line, CsvColumnMapping mapping, Integer vat);

	void save(T entity, long carId, String name);
}
