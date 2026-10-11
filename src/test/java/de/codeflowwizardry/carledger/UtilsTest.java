package de.codeflowwizardry.carledger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class UtilsTest
{
	@Test
	void shouldTestValueIsSet()
	{
		BigDecimal invalidDecimal = null;
		assertFalse(Utils.valueWasSet(invalidDecimal));
		assertTrue(Utils.valueWasSet(BigDecimal.valueOf(10)));
		assertFalse(Utils.valueWasSet(BigDecimal.ZERO));
		assertFalse(Utils.valueWasSet(BigDecimal.valueOf(-10)));
	}

	@Test
	void shouldValidate_atLeastTwoValuesSet()
	{
		assertTrue(Utils.atLeastTwoValuesSet(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
		assertTrue(Utils.atLeastTwoValuesSet(BigDecimal.ONE, BigDecimal.ONE, null));
		assertTrue(Utils.atLeastTwoValuesSet(BigDecimal.ONE, null, BigDecimal.ONE));
		assertTrue(Utils.atLeastTwoValuesSet(null, BigDecimal.ONE, BigDecimal.ONE));
		assertTrue(Utils.atLeastTwoValuesSet(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE));

		assertFalse(Utils.atLeastTwoValuesSet(null, null, BigDecimal.ONE));
		assertFalse(Utils.atLeastTwoValuesSet(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE));
		assertFalse(Utils.atLeastTwoValuesSet(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
	}
}
