package de.codeflowwizardry.carledger.csv;

import java.util.ArrayList;
import java.util.List;

public class CsvImportResult
{
	private int successCount = 0;
	private int skippedCount = 0;
	private int errorCount = 0;
	private final List<RowError> errors = new ArrayList<>();

	public void addSuccess(int rowNumber)
	{
		successCount++;
	}

	public void addSkipped(int rowNumber, String reason)
	{
		skippedCount++;
	}

	public void addError(int rowNumber, String message)
	{
		errorCount++;
		errors.add(new RowError(rowNumber, message));
	}

	public int getSuccessCount()
	{
		return successCount;
	}

	public int getSkippedCount()
	{
		return skippedCount;
	}

	public int getErrorCount()
	{
		return errorCount;
	}

	public List<RowError> getErrors()
	{
		return errors;
	}

	public record RowError(int rowNumber, String message)
	{
	}
}
