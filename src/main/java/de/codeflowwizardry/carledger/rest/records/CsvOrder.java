package de.codeflowwizardry.carledger.rest.records;

public record CsvOrder(int date, int unit, int pricePerUnit, int distance, int estimate)
{
	public CsvOrder()
	{
		this(0, 1, 2, 3, 4);
	}
}
