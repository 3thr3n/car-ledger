package de.codeflowwizardry.carledger.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import de.codeflowwizardry.carledger.exception.CsvImportException;

class CsvFieldExtractorsTest
{
	private static BigDecimal parse(String value)
	{
		return CsvFieldExtractors.getBigDecimal(new String[] {
				value
		}, new CsvColumnMapping(Map.of("total", 0)), "total");
	}

	@Test
	void shouldParseDecimalSeparators()
	{
		assertEquals(new BigDecimal("12.34"), parse("12.34"));
		assertEquals(new BigDecimal("12.34"), parse("12,34"));
		assertEquals(new BigDecimal("12"), parse("12"));
	}

	@Test
	void shouldRemoveThousandSeparators()
	{
		assertEquals(new BigDecimal("1234.56"), parse("1.234,56"));
		assertEquals(new BigDecimal("1234.56"), parse("1,234.56"));
		assertEquals(new BigDecimal("1234.56"), parse("1 234,56"));
		assertEquals(new BigDecimal("1234.56"), parse("1 234,56"));
		assertEquals(new BigDecimal("1234.56"), parse("1'234.56"));
		assertEquals(new BigDecimal("1234567"), parse("1.234.567"));
		assertEquals(new BigDecimal("1234567"), parse("1,234,567"));
		assertEquals(new BigDecimal("1234567.89"), parse("1.234.567,89"));
	}

	@Test
	void shouldReturnZeroForEmptyCell()
	{
		assertEquals(BigDecimal.ZERO, parse(""));
		assertEquals(BigDecimal.ZERO, parse("   "));
	}

	@Test
	void shouldFailForInvalidNumber()
	{
		assertThrows(CsvImportException.class, () -> parse("abc"));
		assertThrows(CsvImportException.class, () -> parse("1,2.3,4"));
	}
}
