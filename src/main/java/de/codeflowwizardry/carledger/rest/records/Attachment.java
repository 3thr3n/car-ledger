package de.codeflowwizardry.carledger.rest.records;

import java.time.OffsetDateTime;

import de.codeflowwizardry.carledger.data.AttachmentEntity;

/**
 * Metadata of an attached file, the content is loaded separately
 */
public record Attachment(Long id, String fileName, String contentType, long size, OffsetDateTime createdAt)
{
	public static Attachment convert(AttachmentEntity entity)
	{
		return new Attachment(entity.getId(), entity.getFileName(), entity.getContentType(), entity.getSize(),
				entity.getCreatedAt());
	}
}
