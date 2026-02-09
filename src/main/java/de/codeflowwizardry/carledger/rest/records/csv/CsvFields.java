package de.codeflowwizardry.carledger.rest.records.csv;

import java.util.Set;

public record CsvFields(Set<String> all, Set<String> required)
{
}
