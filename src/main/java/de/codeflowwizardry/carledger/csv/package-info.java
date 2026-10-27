/**
 * The import of bills from CSV files with a mapping of their columns, and the
 * exports as CSV. {@link de.codeflowwizardry.carledger.csv.CsvImportService}
 * picks the importer of the bill type
 * ({@link de.codeflowwizardry.carledger.csv.importer}),
 * {@link de.codeflowwizardry.carledger.csv.CsvProcessor} reads the rows,
 * converts their units ({@link de.codeflowwizardry.carledger.csv.ImportUnits})
 * and collects the errors per row.
 * {@link de.codeflowwizardry.carledger.csv.CsvExporter} writes the bills in the
 * format of the import,
 * {@link de.codeflowwizardry.carledger.csv.ExpenseExporter} all expenses of a
 * car.
 */
package de.codeflowwizardry.carledger.csv;
