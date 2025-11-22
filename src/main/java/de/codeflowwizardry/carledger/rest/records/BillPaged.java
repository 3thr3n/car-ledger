package de.codeflowwizardry.carledger.rest.records;

import java.util.List;

public record BillPaged(Long total, int page, int size, List<Bill> data)
{
}
