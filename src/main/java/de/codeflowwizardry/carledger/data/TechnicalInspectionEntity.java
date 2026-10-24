package de.codeflowwizardry.carledger.data;

import java.time.LocalDate;

import jakarta.persistence.*;

/**
 * A Hauptuntersuchung (HU) with the AU
 */
@Entity(name = "technical_inspection")
public class TechnicalInspectionEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(insertable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ti_car_id", nullable = false)
	private CarEntity car;

	@Column(name = "ti_date", nullable = false)
	private LocalDate date;

	@Column(name = "ti_odometer")
	private Integer odometer;

	/** e.g. TÜV, DEKRA, GTÜ */
	@Column(name = "ti_testing_body", length = 100)
	private String testingBody;

	@Enumerated(EnumType.STRING)
	@Column(name = "ti_result", nullable = false, length = 20)
	private InspectionResult result;

	@Column(name = "ti_defects", length = 2000)
	private String defects;

	/** the first day of the month the next HU is due */
	@Column(name = "ti_next_due")
	private LocalDate nextDue;

	protected TechnicalInspectionEntity()
	{
	}

	public TechnicalInspectionEntity(CarEntity car)
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

	public LocalDate getDate()
	{
		return date;
	}

	public void setDate(LocalDate date)
	{
		this.date = date;
	}

	public Integer getOdometer()
	{
		return odometer;
	}

	public void setOdometer(Integer odometer)
	{
		this.odometer = odometer;
	}

	public String getTestingBody()
	{
		return testingBody;
	}

	public void setTestingBody(String testingBody)
	{
		this.testingBody = testingBody;
	}

	public InspectionResult getResult()
	{
		return result;
	}

	public void setResult(InspectionResult result)
	{
		this.result = result;
	}

	public String getDefects()
	{
		return defects;
	}

	public void setDefects(String defects)
	{
		this.defects = defects;
	}

	public LocalDate getNextDue()
	{
		return nextDue;
	}

	public void setNextDue(LocalDate nextDue)
	{
		this.nextDue = nextDue;
	}
}
