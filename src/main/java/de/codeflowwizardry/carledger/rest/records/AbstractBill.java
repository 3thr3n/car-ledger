package de.codeflowwizardry.carledger.rest.records;

/**
 * Parent of all bill responses
 */
public abstract class AbstractBill
{
	private long attachments;
	private Long photo;

	public abstract Long getId();

	/**
	 * @return amount of attached files, only set in the lists
	 */
	public long getAttachments()
	{
		return attachments;
	}

	public void setAttachments(long attachments)
	{
		this.attachments = attachments;
	}

	/**
	 * @return the first attached photo, shown small in the lists, only set
	 *         there; <code>null</code> without photos (e.g. only PDFs)
	 */
	public Long getPhoto()
	{
		return photo;
	}

	public void setPhoto(Long photo)
	{
		this.photo = photo;
	}
}
