package de.codeflowwizardry.carledger.data;

import jakarta.persistence.*;

/**
 * Content of an attachment kept in the database
 */
@Entity(name = "AttachmentContent")
@Table(name = "attachment_content")
public class AttachmentContentEntity
{
	@Id
	@Column(name = "attachment_id")
	private Long attachmentId;

	@Column(name = "ac_data", nullable = false)
	private byte[] data;

	public AttachmentContentEntity()
	{
	}

	public AttachmentContentEntity(Long attachmentId, byte[] data)
	{
		this.attachmentId = attachmentId;
		this.data = data;
	}

	public Long getAttachmentId()
	{
		return attachmentId;
	}

	public byte[] getData()
	{
		return data;
	}
}
