package de.codeflowwizardry.carledger.papers;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfImportedPage;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfWriter;

import de.codeflowwizardry.carledger.data.AttachmentEntity;
import de.codeflowwizardry.carledger.data.ModificationEntity;
import de.codeflowwizardry.carledger.data.TechnicalInspectionEntity;
import de.codeflowwizardry.carledger.data.UnitSystem;

/**
 * The papers of a car as PDF, A4: the car, its modifications with their
 * approval and the HUs on the first pages, then each file on pages of its own
 * with a caption, a PDF page by page and a photo scaled to the page. In the
 * language and the units of the account, the texts are
 * src/main/resources/i18n/car-papers.
 */
public final class CarPapersPdfWriter
{
	private static final double KM_PER_MILE = 1.609344;
	private static final Color GREY = new Color(0x80, 0x80, 0x80);
	private static final Color LINE = new Color(0xd0, 0xd0, 0xd0);
	private static final Color HEAD = new Color(0xee, 0xee, 0xee);
	private static final Color WARNING = new Color(0xb4, 0x53, 0x09);
	/** room for the caption above a file */
	private static final float CAPTION = 22;

	private final CarPapers papers;
	private final ResourceBundle texts;
	private final Locale locale;
	private final DateTimeFormatter dates;
	private final DateTimeFormatter months;
	private final NumberFormat numbers;
	private final boolean miles;

	private CarPapersPdfWriter(CarPapers papers)
	{
		this.papers = papers;
		this.locale = Locale.forLanguageTag(papers.language());
		this.texts = ResourceBundle.getBundle("i18n.car-papers", locale);
		this.dates = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale);
		this.months = DateTimeFormatter.ofPattern("MMMM yyyy", locale);
		this.numbers = NumberFormat.getIntegerInstance(locale);
		this.miles = papers.units() != UnitSystem.METRIC;
	}

	public static byte[] write(CarPapers papers)
	{
		return new CarPapersPdfWriter(papers).write();
	}

	private byte[] write()
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Document document = new Document(PageSize.A4, 36, 36, 36, 44);
		PdfWriter writer = PdfWriter.getInstance(document, out);
		writer.setPageEvent(new Footer());
		document.addTitle(text("title") + " " + papers.carName());
		document.addCreator("CarLedger");
		document.open();

		document.add(new Paragraph(text("title"), font(16, Font.BOLD, null)));
		document.add(header());
		document.add(heading("modifications"));
		if (papers.parts().isEmpty())
		{
			document.add(new Paragraph(text("noModifications"), font(9, Font.NORMAL, null)));
		} else
		{
			document.add(parts());
		}
		document.add(heading("inspections"));
		if (papers.inspections().isEmpty())
		{
			document.add(new Paragraph(text("noInspections"), font(9, Font.NORMAL, null)));
		} else
		{
			document.add(inspections());
		}

		for (CarPapers.Part part : papers.parts())
		{
			for (CarPapers.File file : part.documents())
			{
				append(document, writer, file, text("captionDocument", part.modification().getPart(),
						text("kind." + file.attachment().getKind()), file.attachment().getFileName()));
			}
		}
		for (CarPapers.Inspection inspection : papers.inspections())
		{
			for (CarPapers.File file : inspection.files())
			{
				append(document, writer, file, text("captionInspection",
						dates.format(inspection.inspection().getDate()), file.attachment().getFileName()));
			}
		}
		document.close();
		return out.toByteArray();
	}

	private Paragraph header()
	{
		Paragraph header = new Paragraph();
		header.setSpacingBefore(4);
		Font font = font(9, Font.NORMAL, null);
		List<String> lines = new ArrayList<>();
		lines.add(text("car") + ": " + papers.carName());
		if (present(papers.licensePlate()))
		{
			lines.add(text("licensePlate") + ": " + papers.licensePlate());
		}
		if (present(papers.vin()))
		{
			lines.add(text("vin") + ": " + papers.vin());
		}
		lines.add(text("content") + ": " + text(papers.history() ? "content.history" : "content.current"));
		lines.add(text("createdAt", dates.format(papers.createdAt())));
		header.add(new Phrase(String.join("\n", lines), font));
		header.add(new Phrase("\n" + text("note"), font(8, Font.ITALIC, GREY)));
		return header;
	}

	private Paragraph heading(String key)
	{
		Paragraph heading = new Paragraph(text(key), font(12, Font.BOLD, null));
		heading.setSpacingBefore(14);
		heading.setSpacingAfter(6);
		return heading;
	}

	private PdfPTable parts()
	{
		PdfPTable table = table(new float[] {
				27, 23, 25, 25
		}, "part", "approval", "abnahme", "installed");
		for (CarPapers.Part part : papers.parts())
		{
			ModificationEntity modification = part.modification();
			Color color = modification.isInstalled() ? null : GREY;
			Font font = font(8, Font.NORMAL, color);

			Phrase name = new Phrase(modification.getPart(), font(8, Font.BOLD, color));
			if (present(modification.getManufacturer()))
			{
				name.add(new Phrase("\n" + modification.getManufacturer(), font));
			}
			table.addCell(cell(name));
			table.addCell(cell(lines(text("approvalType." + modification.getApprovalType()),
					modification.getApprovalNumber()), font));
			table.addCell(abnahme(modification, color));
			table.addCell(cell(lines(when(modification.getInstalledDate(), modification.getInstalledOdometer()),
					modification.isInstalled() ? null
							: text("removed", when(modification.getRemovedDate(),
									modification.getRemovedOdometer()))),
					font));

			List<String> details = new ArrayList<>();
			if (present(modification.getConditions()))
			{
				details.add(text("conditions", modification.getConditions()));
			}
			if (!part.documents().isEmpty())
			{
				details.add(text("documents", String.join(", ", part.documents().stream()
						.map(file -> text("kind." + file.attachment().getKind()) + " ("
								+ file.attachment().getFileName() + ")")
						.toList())));
			}
			if (!details.isEmpty())
			{
				PdfPCell cell = cell(String.join("\n", details), font(8, Font.NORMAL, color == null ? null : color));
				cell.setColspan(4);
				cell.setPaddingLeft(10);
				table.addCell(cell);
			}
		}
		return table;
	}

	private PdfPCell abnahme(ModificationEntity modification, Color color)
	{
		if (!modification.getApprovalType().needsInspection())
		{
			return cell(text("abnahmeNotNeeded"), font(8, Font.NORMAL, GREY));
		}
		Phrase phrase = new Phrase();
		if (modification.getInspectionDate() == null)
		{
			phrase.add(new Phrase(text("abnahmeOpen"), font(8, Font.BOLD, color == null ? WARNING : color)));
		} else
		{
			phrase.add(new Phrase(lines(dates.format(modification.getInspectionDate()), modification.getTestingBody()),
					font(8, Font.NORMAL, color)));
		}
		phrase.add(new Phrase("\n" + text(modification.isRegistered() ? "registered" : "notRegistered"),
				font(8, Font.NORMAL, GREY)));
		return cell(phrase);
	}

	private PdfPTable inspections()
	{
		PdfPTable table = table(new float[] {
				18, 22, 22, 18, 20
		}, "date", "result", "testingBody", "odometer", "nextDue");
		for (CarPapers.Inspection each : papers.inspections())
		{
			TechnicalInspectionEntity inspection = each.inspection();
			Font font = font(8, Font.NORMAL, null);
			table.addCell(cell(dates.format(inspection.getDate()), font(8, Font.BOLD, null)));
			table.addCell(cell(text("result." + inspection.getResult()),
					font(8, Font.NORMAL, inspection.getResult().passed() ? null : WARNING)));
			table.addCell(cell(inspection.getTestingBody(), font));
			table.addCell(cell(odometer(inspection.getOdometer()), font));
			table.addCell(cell(inspection.getNextDue() == null ? ""
					: months.format(YearMonth.from(inspection.getNextDue())), font));
			if (present(inspection.getDefects()))
			{
				PdfPCell cell = cell(text("defects", inspection.getDefects()), font);
				cell.setColspan(5);
				cell.setPaddingLeft(10);
				table.addCell(cell);
			}
		}
		return table;
	}

	private PdfPTable table(float[] widths, String... columns)
	{
		PdfPTable table = new PdfPTable(widths);
		table.setWidthPercentage(100);
		table.setHeaderRows(1);
		for (String column : columns)
		{
			PdfPCell cell = cell(text(column), font(8, Font.BOLD, null));
			cell.setBackgroundColor(HEAD);
			table.addCell(cell);
		}
		return table;
	}

	/**
	 * A file on pages of its own: each page of a PDF scaled to the page, a photo
	 * scaled to it, a note for a file that can't be included (e.g. WebP)
	 */
	private void append(Document document, PdfWriter writer, CarPapers.File file, String caption)
	{
		AttachmentEntity attachment = file.attachment();
		byte[] content = file.content().get();
		if ("application/pdf".equals(attachment.getContentType()))
		{
			try
			{
				PdfReader reader = new PdfReader(content);
				int pages = reader.getNumberOfPages();
				for (int page = 1; page <= pages; page++)
				{
					document.newPage();
					caption(writer, document, pages == 1 ? caption
							: caption + " (" + text("captionPage", page, pages) + ")");
					place(writer, document, writer.getImportedPage(reader, page), reader.getPageSize(page),
							reader.getPageRotation(page));
				}
				return;
			}
			catch (IOException | RuntimeException e)
			{
				notIncluded(document, caption, "PDF");
				return;
			}
		}
		try
		{
			Image image = Image.getInstance(content);
			document.newPage();
			caption(writer, document, caption);
			float width = document.right() - document.left();
			float height = document.top() - document.bottom() - CAPTION;
			// a large photo shrinks to the page, a small one keeps its size
			if (image.getWidth() > width || image.getHeight() > height)
			{
				image.scaleToFit(width, height);
			}
			image.setAbsolutePosition(document.left()
					+ (document.right() - document.left() - image.getScaledWidth()) / 2,
					document.top() - CAPTION - image.getScaledHeight());
			document.add(image);
		}
		catch (IOException | RuntimeException e)
		{
			notIncluded(document, caption, attachment.getContentType());
		}
	}

	private void notIncluded(Document document, String caption, String type)
	{
		document.newPage();
		document.add(new Paragraph(caption, font(10, Font.BOLD, null)));
		document.add(new Paragraph(text("notIncluded", type), font(9, Font.NORMAL, GREY)));
	}

	private void caption(PdfWriter writer, Document document, String caption)
	{
		ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
				new Phrase(caption, font(10, Font.BOLD, null)), document.left(), document.top() - 10, 0);
	}

	/**
	 * Scales the page of another PDF into the page below the caption, turned as
	 * it is shown
	 */
	private static void place(PdfWriter writer, Document document, PdfImportedPage page, Rectangle size,
			int rotation)
	{
		boolean turned = rotation % 180 != 0;
		float width = turned ? size.getHeight() : size.getWidth();
		float height = turned ? size.getWidth() : size.getHeight();
		float areaWidth = document.right() - document.left();
		float areaHeight = document.top() - document.bottom() - CAPTION;
		float s = Math.min(1, Math.min(areaWidth / width, areaHeight / height));
		float x = document.left() + (areaWidth - width * s) / 2;
		float y = document.top() - CAPTION - height * s;
		// the origin of the page of the other PDF isn't always 0, 0
		x -= size.getLeft() * s;
		y -= size.getBottom() * s;

		PdfContentByte content = writer.getDirectContent();
		switch ((rotation % 360 + 360) % 360)
		{
			case 90 -> content.addTemplate(page, 0, -s, s, 0, x, y + s * size.getWidth());
			case 180 -> content.addTemplate(page, -s, 0, 0, -s, x + s * size.getWidth(), y + s * size.getHeight());
			case 270 -> content.addTemplate(page, 0, s, -s, 0, x + s * size.getHeight(), y);
			default -> content.addTemplate(page, s, 0, 0, s, x, y);
		}
	}

	/**
	 * @return e.g. "Apr 28, 2025 · 41,000 km", what is known of it
	 */
	private String when(LocalDate date, Integer km)
	{
		List<String> parts = new ArrayList<>();
		if (date != null)
		{
			parts.add(dates.format(date));
		}
		if (km != null)
		{
			parts.add(odometer(km));
		}
		return String.join(" · ", parts);
	}

	/**
	 * @return in the unit of the account
	 */
	private String odometer(Integer km)
	{
		if (km == null)
		{
			return "";
		}
		return numbers.format(miles ? Math.round(km / KM_PER_MILE) : km) + (miles ? " mi" : " km");
	}

	private static String lines(String first, String second)
	{
		return present(second) ? first + "\n" + second : first;
	}

	private static boolean present(String value)
	{
		return value != null && !value.isBlank();
	}

	private static PdfPCell cell(String text, Font font)
	{
		return cell(new Phrase(text == null ? "" : text, font));
	}

	private static PdfPCell cell(Phrase phrase)
	{
		PdfPCell cell = new PdfPCell(phrase);
		cell.setPadding(3);
		cell.setBorder(Rectangle.BOTTOM);
		cell.setBorderColor(LINE);
		return cell;
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
	 * The note and the page on every page, also below the appended files
	 */
	private final class Footer extends PdfPageEventHelper
	{
		@Override
		public void onEndPage(PdfWriter writer, Document document)
		{
			Font font = font(7, Font.NORMAL, GREY);
			float y = document.bottom() - 20;
			ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
					new Phrase(text("footer", papers.carName()), font), document.left(), y, 0);
			ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
					new Phrase(text("page", writer.getPageNumber()), font), document.right(), y, 0);
		}
	}
}
