package de.codeflowwizardry.carledger.csv;

/**
 * Text cells of the CSV exports. A spreadsheet runs a cell that starts with
 * <code>= + - @</code>, a tab or a carriage return as a formula, e.g.
 * <code>=HYPERLINK(...)</code> in a description an editor entered into a
 * shared car; such a cell gets a leading <code>'</code>, which the
 * spreadsheet shows as text (OWASP CSV injection).
 */
public final class CsvText
{
	private CsvText()
	{
	}

	/**
	 * @return the value as a cell, empty for <code>null</code>; numbers and
	 *         dates as they are, a negative number stays a number
	 */
	public static String cell(Object value)
	{
		if (value instanceof String text)
		{
			return text(text);
		}
		return value == null ? "" : value.toString();
	}

	/**
	 * @return the text, with a leading <code>'</code> if a spreadsheet would run
	 *         it as a formula
	 */
	public static String text(String text)
	{
		if (text == null || text.isEmpty())
		{
			return "";
		}
		return switch (text.charAt(0))
		{
			case '=', '+', '-', '@', '\t', '\r' -> "'" + text;
			default -> text;
		};
	}
}
