package de.codeflowwizardry.carledger.csv;

import java.math.BigInteger;

import de.codeflowwizardry.carledger.rest.records.input.AbstractBillInput;

public interface CsvImporter<T extends AbstractBillInput> extends ValidatableImporter
{
	T parseRow(String[] line, CsvColumnMapping mapping, BigInteger vat);

	void save(T entity, long carId, String name);
}
