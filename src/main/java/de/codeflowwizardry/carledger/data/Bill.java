package de.codeflowwizardry.carledger.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import de.codeflowwizardry.carledger.rest.records.BillInputPojo;
import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {
		"_day", "unit", "distance", "car_id"
}))
@SequenceGenerator(name = "sequence_bill", allocationSize = 1, initialValue = 5, sequenceName = "sequence_bill")
public class Bill
{
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_bill")
	@Column(insertable = false, updatable = false)
	private Long id;
	@Column(name = "_day", updatable = false)
	private LocalDate day = LocalDate.now();
	@Column(updatable = false)
	private BigDecimal unit = BigDecimal.ZERO;
	@Column(name = "price_per_unit")
	private BigDecimal pricePerUnit = BigDecimal.ZERO;
	private BigDecimal distance = BigDecimal.ZERO;
	private BigDecimal estimate = BigDecimal.ZERO;

	@ManyToOne(optional = false)
	private Car car;

	public Bill()
	{
	}

	public Bill(BillInputPojo billPojo)
	{
		this.day = billPojo.day();
		this.distance = Objects.requireNonNullElse(billPojo.distance(), BigDecimal.ZERO);
		this.estimate = Objects.requireNonNullElse(billPojo.distance(), BigDecimal.ZERO);
		this.pricePerUnit = Objects.requireNonNullElse(billPojo.distance(), BigDecimal.ZERO);
		this.unit = Objects.requireNonNullElse(billPojo.distance(), BigDecimal.ZERO);
	}

	public Long getId()
	{
		return id;
	}

	public LocalDate getDay()
	{
		return day;
	}

	public void setDay(LocalDate day)
	{
		this.day = day;
	}

	public BigDecimal getUnit()
	{
		return unit;
	}

	public void setUnit(BigDecimal unit)
	{
		this.unit = unit;
	}

	public BigDecimal getPricePerUnit()
	{
		return pricePerUnit;
	}

	public void setPricePerUnit(BigDecimal pricePerUnit)
	{
		this.pricePerUnit = pricePerUnit;
	}

	public BigDecimal getDistance()
	{
		return distance;
	}

	public void setDistance(BigDecimal distance)
	{
		this.distance = distance;
	}

	public BigDecimal getEstimate()
	{
		return estimate;
	}

	public void setEstimate(BigDecimal estimate)
	{
		this.estimate = estimate;
	}

	public Car getCar()
	{
		return car;
	}

	public void setCar(Car car)
	{
		this.car = car;
	}

	public Long getCarId()
	{
		return car.getId();
	}
}
