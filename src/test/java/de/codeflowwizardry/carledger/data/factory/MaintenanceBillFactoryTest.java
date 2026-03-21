package de.codeflowwizardry.carledger.data.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.codeflowwizardry.carledger.rest.car.maintenance.MaintenanceBillInput;

class MaintenanceBillFactoryTest
{
	@Test
	void shouldValidate()
	{
		// when
		MaintenanceBillInput minimalValidation = new MaintenanceBillInput(BigDecimal.TEN, BigInteger.ZERO,
				LocalDate.now(), null, null, null, null, null);
		List<String> validate = minimalValidation.validate();
		assertEquals(0, validate.size());
	}

	@Test
	void shouldFailValidating()
	{
		// when date not set
		MaintenanceBillInput missingDate = new MaintenanceBillInput(BigDecimal.TEN, BigInteger.ZERO, null,
				null, null, null, null, null);
		List<String> dateValidate = missingDate.validate();
		assertLinesMatch(List.of("Date must be set!"), dateValidate);

		// when vat not set
		MaintenanceBillInput missingVat = new MaintenanceBillInput(BigDecimal.TEN, null, LocalDate.now(),
				null, null, null, null, null);
		List<String> vatValidate = missingVat.validate();
		assertLinesMatch(List.of("Vat rate must be set!"), vatValidate);

		// when total not set
		MaintenanceBillInput missingTotal = new MaintenanceBillInput(null, BigInteger.TEN, LocalDate.now(),
				null, null, null, null, null);
		List<String> totalValidate = missingTotal.validate();
		assertLinesMatch(List.of("Total must be set!"), totalValidate);
	}
}
