package de.codeflowwizardry.carledger.csv;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;

import de.codeflowwizardry.carledger.data.AbstractBillEntity;
import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.data.MaintenanceBillEntity;
import de.codeflowwizardry.carledger.data.MiscellaneousBillEntity;

/**
 * Writes bills as CSV in the format of the import: the header names the fields
 * of the import, dates are ISO dates and numbers use a dot as decimal
 * separator. An exported file can be imported again without mapping the columns
 * by hand.
 */
public class CsvExporter<E extends AbstractBillEntity>
{
	public static final CsvExporter<FuelBillEntity> FUEL = new CsvExporter<FuelBillEntity>()
			.column("unit", FuelBillEntity::getUnit)
			.column("pricePerUnit", FuelBillEntity::getPricePerUnit)
			.column("distance", FuelBillEntity::getDistance)
			.column("estimateConsumption", FuelBillEntity::getEstimate);

	public static final CsvExporter<MaintenanceBillEntity> MAINTENANCE = new CsvExporter<MaintenanceBillEntity>()
			.column("workshop", MaintenanceBillEntity::getWorkshop)
			.column("description", MaintenanceBillEntity::getDescription)
			.column("odometer", MaintenanceBillEntity::getOdometer)
			.column("laborCost", MaintenanceBillEntity::getLaborCost)
			.column("partsCost", MaintenanceBillEntity::getPartsCost);

	public static final CsvExporter<MiscellaneousBillEntity> MISCELLANEOUS = new CsvExporter<MiscellaneousBillEntity>()
			.column("description", MiscellaneousBillEntity::getDescription);

	private final Map<String, Function<E, Object>> columns = new LinkedHashMap<>();

	private CsvExporter()
	{
		// the fields of every bill
		column("date", bill -> bill.getBill().getDate());
		column("total", bill -> bill.getBill().getTotal());
		column("vatRate", bill -> bill.getBill().getVatRate());
	}

	private CsvExporter<E> column(String field, Function<E, Object> value)
	{
		columns.put(field, value);
		return this;
	}

	public List<String> getFields()
	{
		return List.copyOf(columns.keySet());
	}

	public String write(List<E> bills)
	{
		StringWriter out = new StringWriter();
		try (ICSVWriter writer = new CSVWriter(out, ICSVWriter.DEFAULT_SEPARATOR, ICSVWriter.DEFAULT_QUOTE_CHARACTER,
				ICSVWriter.DEFAULT_ESCAPE_CHARACTER, "\n"))
		{
			writer.writeNext(columns.keySet().toArray(String[]::new), false);
			for (E bill : bills)
			{
				// quoted only if needed, e.g. a description with a comma
				writer.writeNext(columns.values().stream()
						.map(value -> format(value.apply(bill)))
						.toArray(String[]::new), false);
			}
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return out.toString();
	}

	private static String format(Object value)
	{
		return switch (value)
		{
			case null -> "";
			// no exponent, e.g. 1E+2
			case BigDecimal number -> number.toPlainString();
			default -> value.toString();
		};
	}
}
