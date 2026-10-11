package de.codeflowwizardry.carledger.data;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity(name = "bill")
public class BillEntity extends AbstractBillEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(insertable = false, updatable = false)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "b_type", nullable = false, length = 50)
	private BillType type;

	@Column(name = "b_date", nullable = false)
	private LocalDate date = LocalDate.now();

	@Column(name = "b_total", nullable = false)
	private BigDecimal total = BigDecimal.ZERO;

	@Column(name = "b_vat_rate", nullable = false)
	private int vatRate;

	@Column(name = "b_net_amount", nullable = false)
	private BigDecimal netAmount = BigDecimal.ZERO;

	@Column(name = "b_ust_amount", nullable = false)
	private BigDecimal ustAmount = BigDecimal.ZERO;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "b_car_id", nullable = false)
	protected CarEntity car;

	// lazy, otherwise every loaded bill triggers three extra selects; fetch join
	// them where needed
	@OneToOne(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private FuelBillEntity fuelBill;

	@OneToOne(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private MaintenanceBillEntity maintenanceBill;

	@OneToOne(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private MiscellaneousBillEntity miscellaneousBill;

	public BillEntity()
	{
	}

	@Override
	public BillEntity getBill()
	{
		return this;
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public BillType getType()
	{
		return type;
	}

	public void setType(BillType type)
	{
		this.type = type;
	}

	public LocalDate getDate()
	{
		return date;
	}

	public void setDate(LocalDate date)
	{
		this.date = date;
	}

	public BigDecimal getTotal()
	{
		return total;
	}

	public void setTotal(BigDecimal total)
	{
		this.total = total;
	}

	public BigDecimal getNetAmount()
	{
		return netAmount;
	}

	public void setNetAmount(BigDecimal netAmount)
	{
		this.netAmount = netAmount;
	}

	public BigDecimal getUstAmount()
	{
		return ustAmount;
	}

	public void setUstAmount(BigDecimal ustAmount)
	{
		this.ustAmount = ustAmount;
	}

	public int getVatRate()
	{
		return vatRate;
	}

	public void setVatRate(int vatRate)
	{
		this.vatRate = vatRate;
	}

	public CarEntity getCar()
	{
		return car;
	}

	public void setCar(CarEntity carEntity)
	{
		this.car = carEntity;
	}

	public Long getCarId()
	{
		return car.getId();
	}

	public FuelBillEntity getFuelBill()
	{
		return fuelBill;
	}

	public MaintenanceBillEntity getMaintenanceBill()
	{
		return maintenanceBill;
	}

	public MiscellaneousBillEntity getMiscellaneousBill()
	{
		return miscellaneousBill;
	}
}
