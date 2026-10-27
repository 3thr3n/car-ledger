package de.codeflowwizardry.carledger.tripexport;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

import de.codeflowwizardry.carledger.data.TripEntity;
import de.codeflowwizardry.carledger.data.TripType;
import de.codeflowwizardry.carledger.data.UnitSystem;
import de.codeflowwizardry.carledger.rest.car.trip.Trip;
import de.codeflowwizardry.carledger.tripexport.TripExport.Sum;

/**
 * The trip log as PDF (Fahrtenbuch), A4 landscape: one line per trip, the
 * oldest first, the deleted ones grey with the reason, the trips entered later
 * with the time they were entered, then the sums per type. In the language and
 * the units of the account, the texts are src/main/resources/i18n/trip-export.
 */
public final class TripPdfWriter
{
	private static final double KM_PER_MILE = 1.609344;
	private static final float[] WIDTHS = {
			12, 7, 7, 7, 7, 13, 15, 11, 8, 13
	};
	private static final Color GREY = new Color(0x80, 0x80, 0x80);
	private static final Color LINE = new Color(0xd0, 0xd0, 0xd0);
	private static final Color HEAD = new Color(0xee, 0xee, 0xee);

	private final TripExport export;
	private final ResourceBundle texts;
	private final Locale locale;
	private final DateTimeFormatter dates;
	private final DateTimeFormatter dateTimes;
	private final DateTimeFormatter times;
	private final NumberFormat numbers;
	private final boolean miles;

	private TripPdfWriter(TripExport export)
	{
		this.export = export;
		this.locale = Locale.forLanguageTag(export.language());
		this.texts = ResourceBundle.getBundle("i18n.trip-export", locale);
		this.dates = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);
		this.dateTimes = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
				.withLocale(locale);
		this.times = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale);
		this.numbers = NumberFormat.getIntegerInstance(locale);
		this.miles = export.units() != UnitSystem.METRIC;
	}

	public static byte[] write(TripExport export)
	{
		return new TripPdfWriter(export).write();
	}

	private byte[] write()
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 40);
		PdfWriter writer = PdfWriter.getInstance(document, out);
		writer.setPageEvent(new Footer());
		document.addTitle(text("title") + " " + export.carName());
		document.addCreator("CarLedger");
		document.open();

		document.add(new Paragraph(text("title"), font(16, Font.BOLD, null)));
		document.add(header());
		if (export.trips().isEmpty())
		{
			document.add(new Paragraph(text("empty"), font(9, Font.NORMAL, null)));
		} else
		{
			document.add(trips());
		}
		document.add(sums());
		document.close();
		return out.toByteArray();
	}

	private Paragraph header()
	{
		Paragraph header = new Paragraph();
		header.setSpacingBefore(4);
		header.setSpacingAfter(10);
		Font font = font(9, Font.NORMAL, null);
		List<String> lines = new ArrayList<>();
		lines.add(text("car") + ": " + export.carName());
		if (export.licensePlate() != null && !export.licensePlate().isBlank())
		{
			lines.add(text("licensePlate") + ": " + export.licensePlate());
		}
		lines.add(text("period") + ": " + period());
		lines.add(text("createdAt", dateTimes.format(export.local(export.createdAt()))));
		header.add(new Phrase(String.join("\n", lines), font));
		return header;
	}

	private String period()
	{
		LocalDate from = export.from();
		LocalDate to = export.to();
		if (from != null && to != null)
		{
			return text("periodFromTo", dates.format(from), dates.format(to));
		}
		if (from != null)
		{
			return text("periodFrom", dates.format(from));
		}
		return to != null ? text("periodTo", dates.format(to)) : text("periodAll");
	}

	private PdfPTable trips()
	{
		PdfPTable table = new PdfPTable(WIDTHS);
		table.setWidthPercentage(100);
		table.setHeaderRows(1);
		String unit = " " + unit();
		for (String column : List.of("date", "startOdometer", "endOdometer", "distance", "type", "destination",
				"purpose", "businessPartner", "driver", "notes"))
		{
			PdfPCell cell = cell(text(column), font(8, Font.BOLD, null), alignment(column));
			cell.setBackgroundColor(HEAD);
			table.addCell(cell);
		}
		for (TripEntity trip : export.trips())
		{
			Color color = trip.isDeleted() ? GREY : null;
			Font font = font(8, Font.NORMAL, color);
			// the odometers of a deleted trip don't count
			Font odometer = font(8, trip.isDeleted() ? Font.STRIKETHRU : Font.NORMAL, color);
			int start = distance(trip.getStartOdometer());
			Integer end = trip.getEndOdometer() == null ? null : distance(trip.getEndOdometer());

			table.addCell(cell(time(trip), font, Element.ALIGN_LEFT));
			table.addCell(cell(numbers.format(start) + unit, odometer, Element.ALIGN_RIGHT));
			table.addCell(cell(end == null ? text("open") : numbers.format(end) + unit, odometer, Element.ALIGN_RIGHT));
			table.addCell(cell(end == null ? "" : numbers.format(end - start) + unit, odometer, Element.ALIGN_RIGHT));
			table.addCell(cell(text("type." + trip.getType()), font, Element.ALIGN_LEFT));
			table.addCell(cell(trip.getDestination(), font, Element.ALIGN_LEFT));
			table.addCell(cell(trip.getPurpose(), font, Element.ALIGN_LEFT));
			table.addCell(cell(trip.getBusinessPartner(), font, Element.ALIGN_LEFT));
			table.addCell(cell(Objects.requireNonNullElseGet(trip.getDriverName(), () -> text("deletedDriver")),
					font, Element.ALIGN_LEFT));
			table.addCell(notes(trip));
		}
		return table;
	}

	private static int alignment(String column)
	{
		return List.of("startOdometer", "endOdometer", "distance").contains(column) ? Element.ALIGN_RIGHT
				: Element.ALIGN_LEFT;
	}

	/**
	 * @return start and end, the day of the end only if it is another one
	 */
	private String time(TripEntity trip)
	{
		OffsetDateTime start = export.local(trip.getStartedAt());
		OffsetDateTime end = export.local(trip.getEndedAt());
		String text = dateTimes.format(start);
		if (end == null)
		{
			return text;
		}
		return text + "\n– " + (end.toLocalDate().equals(start.toLocalDate()) ? times : dateTimes).format(end);
	}

	private PdfPCell notes(TripEntity trip)
	{
		Phrase notes = new Phrase();
		List<String> lines = new ArrayList<>();
		if (trip.isDeleted())
		{
			notes.add(new Phrase(text("deleted", dateTimes.format(export.local(trip.getDeletedAt())),
					trip.getDeletedBy(), trip.getDeleteReason()), font(8, Font.NORMAL, Color.RED.darker())));
		}
		if (Trip.enteredLater(trip))
		{
			OffsetDateTime entered = trip.getFinishedAt() != null ? trip.getFinishedAt() : trip.getCreatedAt();
			lines.add(text("enteredLater", dateTimes.format(export.local(entered))));
		}
		if (trip.getReviewReason() != null)
		{
			lines.add(text("review." + trip.getReviewReason()));
		}
		if (trip.isRestored())
		{
			lines.add(text("restored"));
		}
		if (!lines.isEmpty())
		{
			notes.add(new Phrase((notes.isEmpty() ? "" : "\n") + String.join("\n", lines),
					font(8, Font.NORMAL, GREY)));
		}
		PdfPCell cell = new PdfPCell(notes);
		style(cell, Element.ALIGN_LEFT);
		return cell;
	}

	private PdfPTable sums()
	{
		PdfPTable table = new PdfPTable(new float[] {
				2, 1, 1
		});
		table.setWidthPercentage(40);
		table.setHorizontalAlignment(Element.ALIGN_LEFT);
		table.setSpacingBefore(14);
		Font head = font(8, Font.BOLD, null);
		Font font = font(8, Font.NORMAL, null);
		for (String column : List.of("sums", "trips", "distance"))
		{
			PdfPCell cell = cell(text(column), head, column.equals("sums") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
			cell.setBackgroundColor(HEAD);
			table.addCell(cell);
		}
		Map<TripType, Sum> sums = export.sums();
		int trips = 0;
		int km = 0;
		for (Map.Entry<TripType, Sum> sum : sums.entrySet())
		{
			table.addCell(cell(text("type." + sum.getKey()), font, Element.ALIGN_LEFT));
			table.addCell(cell(numbers.format(sum.getValue().trips()), font, Element.ALIGN_RIGHT));
			table.addCell(cell(numbers.format(distance(sum.getValue().distance())) + " " + unit(), font,
					Element.ALIGN_RIGHT));
			trips += sum.getValue().trips();
			km += sum.getValue().distance();
		}
		table.addCell(cell(text("total"), head, Element.ALIGN_LEFT));
		table.addCell(cell(numbers.format(trips), head, Element.ALIGN_RIGHT));
		table.addCell(cell(numbers.format(distance(km)) + " " + unit(), head, Element.ALIGN_RIGHT));
		return table;
	}

	private PdfPCell cell(String text, Font font, int alignment)
	{
		PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
		style(cell, alignment);
		return cell;
	}

	private static void style(PdfPCell cell, int alignment)
	{
		cell.setHorizontalAlignment(alignment);
		cell.setPadding(3);
		cell.setBorder(Rectangle.BOTTOM);
		cell.setBorderColor(LINE);
	}

	/**
	 * @return in the unit of the account, a sum is converted once and not per
	 *         trip
	 */
	private int distance(int km)
	{
		return miles ? (int) Math.round(km / KM_PER_MILE) : km;
	}

	private String unit()
	{
		return miles ? "mi" : "km";
	}

	private static Font font(float size, int style, Color color)
	{
		return new Font(Font.HELVETICA, size, style, color);
	}

	private String text(String key, Object... arguments)
	{
		String text = texts.getString(key);
		return arguments.length == 0 ? text : new MessageFormat(text, locale).format(arguments);
	}

	/**
	 * The page and the note on every page
	 */
	private final class Footer extends PdfPageEventHelper
	{
		@Override
		public void onEndPage(PdfWriter writer, Document document)
		{
			Font font = font(7, Font.NORMAL, GREY);
			float y = document.bottom() - 20;
			ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
					new Phrase(text("footer"), font), document.left(), y, 0);
			ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
					new Phrase(text("page", writer.getPageNumber()), font), document.right(), y, 0);
		}
	}
}
