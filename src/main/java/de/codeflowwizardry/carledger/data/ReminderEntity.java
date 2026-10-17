package de.codeflowwizardry.carledger.data;

import java.time.LocalDate;

import jakarta.persistence.*;

@Entity(name = "reminder")
public class ReminderEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(insertable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rm_car_id", nullable = false)
	private CarEntity car;

	@Column(name = "rm_title", nullable = false, length = 100)
	private String title;

	@Column(name = "rm_note", length = 500)
	private String note;

	@Column(name = "rm_due_date")
	private LocalDate dueDate;

	@Column(name = "rm_due_odometer")
	private Integer dueOdometer;

	@Column(name = "rm_interval_months")
	private Integer intervalMonths;

	@Column(name = "rm_interval_km")
	private Integer intervalKm;

	/**
	 * The due date and odometer it was mailed for, see {@link #dueKey()}
	 */
	@Column(name = "rm_notified_for", length = 40)
	private String notifiedFor;

	protected ReminderEntity()
	{
	}

	public ReminderEntity(CarEntity car)
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

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getNote()
	{
		return note;
	}

	public void setNote(String note)
	{
		this.note = note;
	}

	public LocalDate getDueDate()
	{
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate)
	{
		this.dueDate = dueDate;
	}

	public Integer getDueOdometer()
	{
		return dueOdometer;
	}

	public void setDueOdometer(Integer dueOdometer)
	{
		this.dueOdometer = dueOdometer;
	}

	public Integer getIntervalMonths()
	{
		return intervalMonths;
	}

	public void setIntervalMonths(Integer intervalMonths)
	{
		this.intervalMonths = intervalMonths;
	}

	public Integer getIntervalKm()
	{
		return intervalKm;
	}

	public void setIntervalKm(Integer intervalKm)
	{
		this.intervalKm = intervalKm;
	}

	public String getNotifiedFor()
	{
		return notifiedFor;
	}

	/**
	 * @return the due date and odometer, e.g. "2026-12-01|120000": a mail is sent
	 *         once per due, a reminder that was done or changed is mailed again
	 */
	public String dueKey()
	{
		return (dueDate == null ? "" : dueDate.toString()) + "|" + (dueOdometer == null ? "" : dueOdometer);
	}

	/**
	 * @return whether it comes back after it is done
	 */
	public boolean isRepeating()
	{
		return intervalMonths != null || intervalKm != null;
	}
}
