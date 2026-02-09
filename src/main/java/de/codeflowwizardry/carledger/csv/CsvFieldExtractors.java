package de.codeflowwizardry.carledger.csv;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class CsvFieldExtractors
{
	private static final List<DateTimeFormatter> DEFAULT_DATE_FORMATTERS = List.of(
			DateTimeFormatter.ISO_LOCAL_DATE,
			DateTimeFormatter.ofPattern("dd.MM.yyyy"),
			DateTimeFormatter.ofPattern("MM/dd/yyyy"),
			DateTimeFormatter.ofPattern("yyyy-MM-dd"));

	public static String getString(String[] line, CsvColumnMapping mapping, String fieldName)
	{
		Integer index = mapping.get(fieldName);
		if (index == null || index < 0 || index >= line.length)
		{
			return null;
		}
		String value = line[index].trim();
		return value.isEmpty() ? null : value;
	}

	public static LocalDate getLocalDate(String[] line, CsvColumnMapping mapping,
			String fieldName)
	{
		String value = getString(line, mapping, fieldName);
		if (value == null)
		{
			return null;
		}

		for (DateTimeFormatter formatter : DEFAULT_DATE_FORMATTERS)
		{
			try
			{
				return LocalDate.parse(value, formatter);
			}
			catch (DateTimeParseException e)
			{
				// Try next format
			}
		}

		throw new IllegalArgumentException(
				String.format("Could not parse date '%s' for field '%s'", value, fieldName));
	}

	public static BigDecimal getBigDecimal(String[] line, CsvColumnMapping mapping,
			String fieldName)
	{
		String value = getString(line, mapping, fieldName);
		if (value == null)
		{
			return BigDecimal.ZERO;
		}

		try
		{
			// Handle both comma and dot as decimal separator
			String normalized = value.replace(",", ".");
			// Remove any thousand separators
			normalized = normalized.replace(" ", "");
			return new BigDecimal(normalized);
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException(
					String.format("Could not parse number '%s' for field '%s'", value, fieldName));
		}
	}

	public static BigInteger getBigInteger(String[] line, CsvColumnMapping mapping, String fieldName)
	{
		return getBigInteger(line, mapping, fieldName, null);
	}

	public static BigInteger getBigInteger(String[] line, CsvColumnMapping mapping, String fieldName,
			BigInteger defaultValue)
	{
		String value = getString(line, mapping, fieldName);
		if (value == null)
		{
			return defaultValue;
		}

		try
		{
			return BigInteger.valueOf(Integer.parseInt(value));
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException(
					String.format("Could not parse integer '%s' for field '%s'", value, fieldName));
		}
	}

	public static Boolean getBoolean(String[] line, CsvColumnMapping mapping, String fieldName)
	{
		String value = getString(line, mapping, fieldName);
		if (value == null)
		{
			return null;
		}

		return switch (value.toLowerCase())
		{
			case "true", "yes", "1", "y" -> true;
			case "false", "no", "0", "n" -> false;
			default -> throw new IllegalArgumentException(
					String.format("Could not parse boolean '%s' for field '%s'", value, fieldName));
		};
	}
}
