package de.codeflowwizardry.carledger.csv;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;

import de.codeflowwizardry.carledger.data.BillEntity;
import de.codeflowwizardry.carledger.data.BillType;
import de.codeflowwizardry.carledger.data.ChargingBillEntity;
import de.codeflowwizardry.carledger.data.FuelBillEntity;
import de.codeflowwizardry.carledger.data.MaintenanceBillEntity;
import de.codeflowwizardry.carledger.data.RecurringBillPaymentEntity;

/**
 * All expenses of a car in one CSV, for spreadsheets and the total cost of
 * ownership: every bill and the payments of the recurring costs, the oldest
 * first. Not meant to be imported again, the export per type is.
 */
public final class ExpenseExporter
{
	static final List<String> HEADER = List.of("date", "type", "description", "category", "total", "netAmount",
			"vatAmount", "vatRate", "quantity", "unit", "distance");

	private ExpenseExporter()
	{
	}

	/**
	 * @param bills
	 *            with their type specific part
	 * @param payments
	 *            with their recurring bill
	 */
	public static String write(List<BillEntity> bills, List<RecurringBillPaymentEntity> payments)
	{
		List<Row> rows = new ArrayList<>();
		bills.stream().map(ExpenseExporter::row).forEach(rows::add);
		payments.stream().map(ExpenseExporter::row).forEach(rows::add);
		rows.sort(Comparator.comparing(Row::date).thenComparing(Row::type));

		StringWriter out = new StringWriter();
		try (ICSVWriter writer = new CSVWriter(out, ICSVWriter.DEFAULT_SEPARATOR, ICSVWriter.DEFAULT_QUOTE_CHARACTER,
				ICSVWriter.DEFAULT_ESCAPE_CHARACTER, "\n"))
		{
			writer.writeNext(HEADER.toArray(String[]::new), false);
			for (Row row : rows)
			{
				writer.writeNext(row.values().stream().map(ExpenseExporter::format).toArray(String[]::new), false);
			}
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
		return out.toString();
	}

	/**
	 * One line of the CSV, in the order of {@link #HEADER}
	 */
	private record Row(LocalDate date, BillType type, String description, String category, BigDecimal total,
			BigDecimal netAmount, BigDecimal vatAmount, Integer vatRate, BigDecimal quantity, String unit,
			BigDecimal distance)
	{
		List<Object> values()
		{
			return Arrays.asList(date, type, description, category, total, netAmount, vatAmount, vatRate,
					quantity, unit, distance);
		}
	}

	private static Row row(BillEntity bill)
	{
		BigDecimal quantity = null;
		String unit = null;
		BigDecimal distance = null;
		String description = null;
		FuelBillEntity fuel = bill.getFuelBill();
		ChargingBillEntity charging = bill.getChargingBill();
		MaintenanceBillEntity maintenance = bill.getMaintenanceBill();
		if (fuel != null)
		{
			quantity = fuel.getUnit();
			unit = "l";
			distance = fuel.getDistance();
		} else if (charging != null)
		{
			quantity = charging.getUnit();
			unit = "kWh";
			distance = charging.getDistance();
		} else if (maintenance != null)
		{
			description = join(maintenance.getWorkshop(), maintenance.getDescription());
		} else if (bill.getMiscellaneousBill() != null)
		{
			description = bill.getMiscellaneousBill().getDescription();
		}
		return new Row(bill.getDate(), bill.getType(), description, null, bill.getTotal(), bill.getNetAmount(),
				bill.getUstAmount(), bill.getVatRate(), quantity, unit,
				distance != null && distance.signum() != 0 ? distance : null);
	}

	private static Row row(RecurringBillPaymentEntity payment)
	{
		var bill = payment.getBill();
		// recurring costs have no VAT
		return new Row(payment.getDueDate(), BillType.RECURRING, bill.getName(), bill.getCategory().name(),
				payment.getAmount(), null, null, null, null, null, null);
	}

	private static String join(String... parts)
	{
		String joined = Stream.of(parts)
				.filter(Objects::nonNull)
				.filter(part -> !part.isBlank())
				.collect(Collectors.joining(" · "));
		return joined.isEmpty() ? null : joined;
	}

	private static String format(Object value)
	{
		return switch (value)
		{
			case null -> "";
			// no exponent, e.g. 1E+2
			case BigDecimal number -> number.toPlainString();
			default -> CsvText.cell(value);
		};
	}
}
