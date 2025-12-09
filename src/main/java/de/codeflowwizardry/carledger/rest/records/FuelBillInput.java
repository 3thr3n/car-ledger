package de.codeflowwizardry.carledger.rest.records;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

public record FuelBillInput(LocalDate day, BigDecimal distance, BigDecimal unit, BigDecimal pricePerUnit,
                            BigDecimal estimate, BigInteger vatRate)
{
}
