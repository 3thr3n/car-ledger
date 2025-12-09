package de.codeflowwizardry.carledger.data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity(name = "bill")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {
		"b_day", "car_id"
}))
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class BillEntity
{
	public static final BigDecimal GERMAN_UST = BigDecimal.valueOf(1.19);

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(insertable = false, updatable = false)
	private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "b_type", nullable = false)
    private BillType type;

	@Column(name = "b_day", updatable = false)
	private LocalDate day = LocalDate.now();

	@Column(name = "b_total", nullable = false)
	private BigDecimal total;

    @Column(name = "b_vat_rate")
    private BigInteger vatRate = BigInteger.ZERO;

    @Column(name = "b_net_amount")
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(name = "b_ust_amount")
    private BigDecimal ustAmount = BigDecimal.ZERO;

	@ManyToOne(optional = false)
	@JoinColumn(name = "b_car_id", nullable = false)
	protected CarEntity car;

	public BillEntity()
	{
	}

	public Long getId()
	{
		return id;
	}

    public void setId(Long id) {
        this.id = id;
    }

    public BillType getType() {
        return type;
    }

    public void setType(BillType type) {
        this.type = type;
    }

    public LocalDate getDay()
	{
		return day;
	}

	public void setDay(LocalDate day)
	{
		this.day = day;
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

    public BigInteger getVatRate()
    {
        return vatRate;
    }

    public void setVatRate(BigInteger vatRate)
    {
        this.vatRate = vatRate;
    }

    public CarEntity getCar() {
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
}
