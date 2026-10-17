package de.codeflowwizardry.carledger.data;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

/**
 * A file attached to a bill, e.g. the receipt. Only the metadata, the content
 * is in the {@link de.codeflowwizardry.carledger.attachment.AttachmentStorage}
 * named by {@link #getStorage()}.
 */
@Entity(name = "Attachment")
@Table(name = "attachment")
public class AttachmentEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "a_car_id", nullable = false)
	private CarEntity car;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "a_bill_id")
	private BillEntity bill;

	@Column(name = "a_file_name", nullable = false)
	private String fileName;

	@Column(name = "a_content_type", nullable = false)
	private String contentType;

	@Column(name = "a_size", nullable = false)
	private long size;

	@Column(name = "a_storage", nullable = false)
	private String storage;

	@Column(name = "a_storage_key")
	private String storageKey;

	@Column(name = "a_created_at", nullable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	public Long getId()
	{
		return id;
	}

	public CarEntity getCar()
	{
		return car;
	}

	public void setCar(CarEntity car)
	{
		this.car = car;
	}

	public BillEntity getBill()
	{
		return bill;
	}

	public void setBill(BillEntity bill)
	{
		this.bill = bill;
	}

	public String getFileName()
	{
		return fileName;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	public String getContentType()
	{
		return contentType;
	}

	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	public long getSize()
	{
		return size;
	}

	public void setSize(long size)
	{
		this.size = size;
	}

	public String getStorage()
	{
		return storage;
	}

	public void setStorage(String storage)
	{
		this.storage = storage;
	}

	public String getStorageKey()
	{
		return storageKey;
	}

	public void setStorageKey(String storageKey)
	{
		this.storageKey = storageKey;
	}

	public OffsetDateTime getCreatedAt()
	{
		return createdAt;
	}
}
