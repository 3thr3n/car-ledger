package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillInputPojo(LocalDate day, BigDecimal distance, BigDecimal unit, BigDecimal pricePerUnit,
		BigDecimal estimate)
{
}
