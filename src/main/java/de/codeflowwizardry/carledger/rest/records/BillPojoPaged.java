package de.codeflowwizardry.carledger.rest.records;

import java.util.List;

public record BillPojoPaged(Long total, int page, int size, List<BillPojo> data)
{
}
