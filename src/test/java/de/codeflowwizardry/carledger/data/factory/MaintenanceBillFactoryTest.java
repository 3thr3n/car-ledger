package de.codeflowwizardry.carledger.data.factory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.records.input.MaintenanceBillInput;

class MaintenanceBillFactoryTest
{
	@Test
	void shouldValidate()
	{
		// given
		MaintenanceBillFactory factory = new MaintenanceBillFactory(null, null, null);

		// when
		MaintenanceBillInput minimalValidation = new MaintenanceBillInput(BigDecimal.TEN, BigInteger.ZERO,
				LocalDate.now(), null, null, null, null, null);
		assertDoesNotThrow(() -> factory.validate(minimalValidation));
	}

	@Test
	void shouldFailValidating()
	{
		// given
		MaintenanceBillFactory factory = new MaintenanceBillFactory(null, null, null);

		// when date not set
		MaintenanceBillInput noDate = new MaintenanceBillInput(BigDecimal.TEN, BigInteger.ZERO, null,
				null, null, null, null, null);
		assertThrows(IllegalArgumentException.class, () -> factory.validate(noDate));

		// when vat not set
		MaintenanceBillInput noVat = new MaintenanceBillInput(BigDecimal.TEN, null, LocalDate.now(),
				null, null, null, null, null);
		assertThrows(IllegalArgumentException.class, () -> factory.validate(noVat));

		// when total not set
		MaintenanceBillInput noTotal = new MaintenanceBillInput(null, BigInteger.TEN, LocalDate.now(),
				null, null, null, null, null);
		assertThrows(IllegalArgumentException.class, () -> factory.validate(noTotal));
	}
}
