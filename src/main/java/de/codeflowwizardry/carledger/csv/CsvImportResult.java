package de.codeflowwizardry.carledger.csv;

import java.util.ArrayList;
import java.util.List;

public class CsvImportResult
{
	private int successCount = 0;
	private final List<Row> skipped = new ArrayList<>();
	private final List<Row> errors = new ArrayList<>();

	public void addSuccess()
	{
		successCount++;
	}

	public void addSkipped(int rowNumber, String reason)
	{
		skipped.add(new Row(rowNumber, reason));
	}

	public void addError(int rowNumber, String message)
	{
		errors.add(new Row(rowNumber, message));
	}

	public int getSuccessCount()
	{
		return successCount;
	}

	public int getSkippedCount()
	{
		return skipped.size();
	}

	public int getErrorCount()
	{
		return errors.size();
	}

	public List<Row> getSkipped()
	{
		return skipped;
	}

	public List<Row> getErrors()
	{
		return errors;
	}

	public record Row(int rowNumber, String message)
	{
	}
}
