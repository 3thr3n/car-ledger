package de.codeflowwizardry.carledger.rest.records;

import java.util.List;

public record BillPaged<T>(Long total, int page, int size, List<T> data)
{
}
