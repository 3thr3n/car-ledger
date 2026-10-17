package de.codeflowwizardry.carledger.rest.records;

/**
 * Parent of all bill responses
 */
public abstract class AbstractBill
{
	private long attachments;

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
}
