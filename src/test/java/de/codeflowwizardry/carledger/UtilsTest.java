package de.codeflowwizardry.carledger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

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

		BigInteger invalidInteger = null;
		assertFalse(Utils.valueWasSet(invalidInteger));
		assertTrue(Utils.valueWasSet(BigInteger.valueOf(10)));
		assertFalse(Utils.valueWasSet(BigInteger.ZERO));
		assertFalse(Utils.valueWasSet(BigInteger.valueOf(-10)));
	}

	@Test
	void shouldValidate_atLeastTwoNotNull()
	{
		assertTrue(Utils.atLeastTwoNotNull(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
		assertTrue(Utils.atLeastTwoNotNull(BigDecimal.ZERO, BigDecimal.ZERO, null));
		assertTrue(Utils.atLeastTwoNotNull(BigDecimal.ZERO, null, BigDecimal.ZERO));
		assertTrue(Utils.atLeastTwoNotNull(null, BigDecimal.ZERO, BigDecimal.ZERO));

		assertFalse(Utils.atLeastTwoNotNull(null, null, BigDecimal.ZERO));
	}
}
