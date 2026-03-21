package de.codeflowwizardry.carledger.data;

import static de.codeflowwizardry.carledger.data.BillInterval.ONCE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity(name = "recurring_bill")
public class RecurringBillEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(insertable = false, updatable = false)
	private Long id;

	@Column(name = "r_name")
	private String name; // e.g. "KFZ-Versicherung"

	@Column(name = "r_description")
	private String description;

	@Column(name = "r_amount", nullable = false)
	private BigDecimal amount;

	@Column(name = "r_category")
	@Enumerated(EnumType.STRING)
	private BillCategory category; // FINANCE, INSURANCE, OTHER

	@Column(name = "r_interval")
	@Enumerated(EnumType.STRING)
	private BillInterval interval; // ONCE, MONTHLY, QUARTERLY, SEMI_ANNUALLY, ANNUALLY

	@Column(name = "r_start_date")
	private LocalDate startDate;

	@Column(name = "r_next_due_date")
	private LocalDate nextDueDate; // computed/stored for easy querying

	@Column(name = "r_end_date")
	private LocalDate endDate; // nullable — open-ended subscriptions

	@ManyToOne(optional = false)
	@JoinColumn(name = "r_car_id", nullable = false)
	private CarEntity car;

	@Column(name = "r_total", nullable = false)
	private BigDecimal total;

	@OneToMany(mappedBy = "bill")
	private final List<RecurringBillPaymentEntity> recurringBillPaymentEntities = new ArrayList<>();

	public RecurringBillEntity()
	{
	}

	public RecurringBillEntity(CarEntity car)
	{
		this.car = car;
	}

	public LocalDate advance()
	{
		if (isOnce())
		{
			endDate = nextDueDate;
			return endDate;
		}
		nextDueDate = switch (interval)
		{
			case MONTHLY -> nextDueDate.plusMonths(1);
			case QUARTERLY -> nextDueDate.plusMonths(3);
			case SEMI_ANNUALLY -> nextDueDate.plusMonths(6);
			case ANNUALLY -> nextDueDate.plusYears(1);
			default -> nextDueDate;
		};

		return nextDueDate;
	}

	public boolean isOnce()
	{
		return interval == ONCE;
	}

	public Long getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public BigDecimal getAmount()
	{
		return amount;
	}

	public void setAmount(BigDecimal amount)
	{
		this.amount = amount;
	}

	public BillCategory getCategory()
	{
		return category;
	}

	public void setCategory(BillCategory category)
	{
		this.category = category;
	}

	public BillInterval getInterval()
	{
		return interval;
	}

	public void setInterval(BillInterval interval)
	{
		this.interval = interval;
	}

	public LocalDate getStartDate()
	{
		return startDate;
	}

	public void setStartDate(LocalDate startDate)
	{
		this.startDate = startDate;
	}

	public LocalDate getNextDueDate()
	{
		return nextDueDate;
	}

	public void setNextDueDate(LocalDate nextDueDate)
	{
		this.nextDueDate = nextDueDate;
	}

	public LocalDate getEndDate()
	{
		return endDate;
	}

	public void setEndDate(LocalDate endDate)
	{
		this.endDate = endDate;
	}

	public BigDecimal getTotal()
	{
		return total;
	}

	public void setTotal(BigDecimal total)
	{
		this.total = total;
	}

	public CarEntity getCar()
	{
		return car;
	}

	public List<RecurringBillPaymentEntity> getRecurringBillPaymentEntities()
	{
		return recurringBillPaymentEntities;
	}
}
