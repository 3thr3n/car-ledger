package de.codeflowwizardry.carledger.rest.records;

import de.codeflowwizardry.carledger.data.BillEntity;

/**
 * Parent of all bill responses
 */
public abstract class AbstractBill
{
	private long attachments;
	private Long photo;
	private String createdBy;

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

	/**
	 * @return the user who created the entry, missing once that account was
	 *         deleted
	 */
	public String getCreatedBy()
	{
		return createdBy;
	}

	/**
	 * @return the bill with the creator of the entity
	 */
	protected static <B extends AbstractBill> B created(B bill, BillEntity entity)
	{
		((AbstractBill) bill).createdBy = entity.getCreatedBy() == null ? null : entity.getCreatedBy().getUserId();
		return bill;
	}
}
