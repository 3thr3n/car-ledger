package de.codeflowwizardry.carledger.data;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

@Entity(name = "modification")
public class ModificationEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(insertable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "md_car_id", nullable = false)
	private CarEntity car;

	/** e.g. the part, the workshop and the Abnahme, a deleted one drops out */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "modification_bill", joinColumns = @JoinColumn(name = "mb_modification_id"), inverseJoinColumns = @JoinColumn(name = "mb_bill_id"))
	private Set<BillEntity> bills = new HashSet<>();

	@Column(name = "md_part", nullable = false, length = 100)
	private String part;

	@Column(name = "md_manufacturer", length = 100)
	private String manufacturer;

	@Enumerated(EnumType.STRING)
	@Column(name = "md_approval_type", nullable = false, length = 20)
	private ApprovalType approvalType;

	/** e.g. the KBA number */
	@Column(name = "md_approval_number", length = 50)
	private String approvalNumber;

	/** Auflagen of the approval */
	@Column(name = "md_conditions", length = 2000)
	private String conditions;

	/** of the Abnahme */
	@Column(name = "md_inspection_date")
	private LocalDate inspectionDate;

	/** e.g. TÜV, DEKRA, GTÜ */
	@Column(name = "md_testing_body", length = 100)
	private String testingBody;

	/** entered in the Zulassungsbescheinigung Teil I */
	@Column(name = "md_registered", nullable = false)
	private boolean registered;

	@Column(name = "md_installed_date")
	private LocalDate installedDate;

	@Column(name = "md_installed_odometer")
	private Integer installedOdometer;

	@Column(name = "md_removed_date")
	private LocalDate removedDate;

	@Column(name = "md_removed_odometer")
	private Integer removedOdometer;

	@Column(name = "md_notes", length = 1000)
	private String notes;

	protected ModificationEntity()
	{
	}

	public ModificationEntity(CarEntity car)
	{
		this.car = car;
	}

	public Long getId()
	{
		return id;
	}

	public CarEntity getCar()
	{
		return car;
	}

	public Set<BillEntity> getBills()
	{
		return bills;
	}

	public void setBills(Collection<BillEntity> bills)
	{
		this.bills.clear();
		this.bills.addAll(bills);
	}

	public String getPart()
	{
		return part;
	}

	public void setPart(String part)
	{
		this.part = part;
	}

	public String getManufacturer()
	{
		return manufacturer;
	}

	public void setManufacturer(String manufacturer)
	{
		this.manufacturer = manufacturer;
	}

	public ApprovalType getApprovalType()
	{
		return approvalType;
	}

	public void setApprovalType(ApprovalType approvalType)
	{
		this.approvalType = approvalType;
	}

	public String getApprovalNumber()
	{
		return approvalNumber;
	}

	public void setApprovalNumber(String approvalNumber)
	{
		this.approvalNumber = approvalNumber;
	}

	public String getConditions()
	{
		return conditions;
	}

	public void setConditions(String conditions)
	{
		this.conditions = conditions;
	}

	public LocalDate getInspectionDate()
	{
		return inspectionDate;
	}

	public void setInspectionDate(LocalDate inspectionDate)
	{
		this.inspectionDate = inspectionDate;
	}

	public String getTestingBody()
	{
		return testingBody;
	}

	public void setTestingBody(String testingBody)
	{
		this.testingBody = testingBody;
	}

	public boolean isRegistered()
	{
		return registered;
	}

	public void setRegistered(boolean registered)
	{
		this.registered = registered;
	}

	public LocalDate getInstalledDate()
	{
		return installedDate;
	}

	public void setInstalledDate(LocalDate installedDate)
	{
		this.installedDate = installedDate;
	}

	public Integer getInstalledOdometer()
	{
		return installedOdometer;
	}

	public void setInstalledOdometer(Integer installedOdometer)
	{
		this.installedOdometer = installedOdometer;
	}

	public LocalDate getRemovedDate()
	{
		return removedDate;
	}

	public void setRemovedDate(LocalDate removedDate)
	{
		this.removedDate = removedDate;
	}

	public Integer getRemovedOdometer()
	{
		return removedOdometer;
	}

	public void setRemovedOdometer(Integer removedOdometer)
	{
		this.removedOdometer = removedOdometer;
	}

	public String getNotes()
	{
		return notes;
	}

	public void setNotes(String notes)
	{
		this.notes = notes;
	}

	/**
	 * @return whether it is still on the car
	 */
	public boolean isInstalled()
	{
		return removedDate == null;
	}
}
