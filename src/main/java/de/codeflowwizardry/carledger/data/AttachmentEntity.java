package de.codeflowwizardry.carledger.data;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

/**
 * A file attached to a bill (e.g. the receipt), to a modification (a document
 * of its approval), to a technical inspection (its report) or to the car (its
 * picture). Only the metadata, the content
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "a_modification_id")
	private ModificationEntity modification;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "a_technical_inspection_id")
	private TechnicalInspectionEntity technicalInspection;

	/** set for the documents of a modification only */
	@Enumerated(EnumType.STRING)
	@Column(name = "a_kind", length = 20)
	private DocumentKind kind;

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

	/** <code>null</code> until generated */
	@Enumerated(EnumType.STRING)
	@Column(name = "a_preview", length = 10)
	private Preview preview;

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

	public ModificationEntity getModification()
	{
		return modification;
	}

	public void setModification(ModificationEntity modification)
	{
		this.modification = modification;
	}

	public TechnicalInspectionEntity getTechnicalInspection()
	{
		return technicalInspection;
	}

	public void setTechnicalInspection(TechnicalInspectionEntity technicalInspection)
	{
		this.technicalInspection = technicalInspection;
	}

	public DocumentKind getKind()
	{
		return kind;
	}

	public void setKind(DocumentKind kind)
	{
		this.kind = kind;
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

	public Preview getPreview()
	{
		return preview;
	}

	public void setPreview(Preview preview)
	{
		this.preview = preview;
	}

	public OffsetDateTime getCreatedAt()
	{
		return createdAt;
	}
}
