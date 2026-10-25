package de.codeflowwizardry.carledger.papers;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import de.codeflowwizardry.carledger.data.AttachmentEntity;
import de.codeflowwizardry.carledger.data.ModificationEntity;
import de.codeflowwizardry.carledger.data.TechnicalInspectionEntity;
import de.codeflowwizardry.carledger.data.UnitSystem;

/**
 * What the PDF of the papers of a car has: its modifications and HUs with their
 * files, see {@link CarPapersPdfWriter}
 *
 * @param history
 *            with the removed parts and all HUs, otherwise the installed parts
 *            and the latest HU
 * @param parts
 *            the installed ones first
 * @param inspections
 *            the latest first
 * @param language
 *            <code>de</code> or <code>en</code>
 */
public record CarPapers(String carName, String licensePlate, String vin, LocalDate createdAt, boolean history,
		List<Part> parts, List<Inspection> inspections, UnitSystem units, String language)
{
	/**
	 * @param documents
	 *            by their kind
	 */
	public record Part(ModificationEntity modification, List<File> documents)
	{
	}

	public record Inspection(TechnicalInspectionEntity inspection, List<File> files)
	{
	}

	/**
	 * @param content
	 *            loaded when the file is written, one at a time
	 */
	public record File(AttachmentEntity attachment, Supplier<byte[]> content)
	{
	}
}
