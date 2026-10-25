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

	@Column(name = "ac_preview")
	private byte[] preview;

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

	public byte[] getPreview()
	{
		return preview;
	}

	public void setPreview(byte[] preview)
	{
		this.preview = preview;
	}
}
