package de.codeflowwizardry.carledger.rest.records;

import java.time.OffsetDateTime;

import de.codeflowwizardry.carledger.data.AttachmentEntity;
import de.codeflowwizardry.carledger.data.DocumentKind;

/**
 * Metadata of an attached file, the content is loaded separately
 *
 * @param kind
 *            of a document of a modification, missing for other files
 */
public record Attachment(Long id, String fileName, String contentType, long size, OffsetDateTime createdAt,
		DocumentKind kind)
{
	public static Attachment convert(AttachmentEntity entity)
	{
		return new Attachment(entity.getId(), entity.getFileName(), entity.getContentType(), entity.getSize(),
				entity.getCreatedAt(), entity.getKind());
	}
}
