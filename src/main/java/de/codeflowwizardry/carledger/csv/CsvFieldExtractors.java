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
			return new BigDecimal(normalizeNumber(value));
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException(
					String.format("Could not parse number '%s' for field '%s'", value, fieldName));
		}
	}

	/**
	 * Accepts comma or dot as decimal separator and removes thousand separators,
	 * e.g. <code>1.234,56</code>,
	 * <code>1,234.56</code> and <code>1 234,56</code> all become
	 * <code>1234.56</code>. A single separator is always
	 * treated as decimal separator.
	 */
	static String normalizeNumber(String value)
	{
		// spaces, no-break spaces and apostrophes are only used as thousand separators
		String normalized = value.replaceAll("[\\s\u00A0\u202F']", "");

		int lastComma = normalized.lastIndexOf(',');
		int lastDot = normalized.lastIndexOf('.');

		if (lastComma >= 0 && lastDot >= 0)
		{
			// the separator that comes last is the decimal separator
			char thousandSeparator = lastComma > lastDot ? '.' : ',';
			normalized = normalized.replace(String.valueOf(thousandSeparator), "");
		} else
		{
			char separator = lastComma >= 0 ? ',' : '.';
			// a separator occurring more than once can only be a thousand separator
			if (normalized.indexOf(separator) != normalized.lastIndexOf(separator))
			{
				normalized = normalized.replace(String.valueOf(separator), "");
			}
		}

		return normalized.replace(',', '.');
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
}
