package de.codeflowwizardry.carledger.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CsvTextTest
{
	@Test
	void shouldKeepAFormulaAsText()
	{
		assertEquals("'=HYPERLINK(\"https://evil.example\",\"x\")",
				CsvText.text("=HYPERLINK(\"https://evil.example\",\"x\")"));
		assertEquals("'+1+2", CsvText.text("+1+2"));
		assertEquals("'-2+3", CsvText.text("-2+3"));
		assertEquals("'@SUM(A1)", CsvText.text("@SUM(A1)"));
		assertEquals("'\t=1", CsvText.text("\t=1"));
		assertEquals("'\r=1", CsvText.text("\r=1"));
	}

	@Test
	void shouldKeepOtherTextAndValues()
	{
		assertEquals("Oil change = 2 l", CsvText.text("Oil change = 2 l"));
		assertEquals("", CsvText.text(null));
		assertEquals("", CsvText.cell(null));
		assertEquals("-12.50", CsvText.cell(new BigDecimal("-12.50")));
		assertEquals("2026-10-27", CsvText.cell(LocalDate.of(2026, 10, 27)));
		assertEquals("'=1", CsvText.cell("=1"));
	}
}
