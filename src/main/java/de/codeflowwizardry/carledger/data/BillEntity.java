package de.codeflowwizardry.carledger.data;

import static de.codeflowwizardry.carledger.StatsCalculator.ONE_HUNDRED;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

import de.codeflowwizardry.carledger.rest.records.BillInput;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity(name = "Bill")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {
		"_day", "unit", "distance", "car_id"
}))
@SequenceGenerator(name = "sequence_bill", allocationSize = 1, initialValue = 5, sequenceName = "sequence_bill")
public class BillEntity
{
	public static final BigDecimal GERMAN_UST = BigDecimal.valueOf(1.19);

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
	private CarEntity car;

	public BillEntity()
	{
	}

	public BillEntity(BillInput billPojo)
	{
		this.day = billPojo.day();
		this.distance = Objects.requireNonNullElse(billPojo.distance(), BigDecimal.ZERO);
		this.estimate = Objects.requireNonNullElse(billPojo.estimate(), BigDecimal.ZERO);
		this.pricePerUnit = Objects.requireNonNullElse(billPojo.pricePerUnit(), BigDecimal.ZERO);
		this.unit = Objects.requireNonNullElse(billPojo.unit(), BigDecimal.ZERO);
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

	public boolean isDistanceSet()
	{
		return !BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).equals(distance);
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

	public BigDecimal getCalculatedPrice(BigDecimal mwst)
	{
		return pricePerUnit
				.divide(mwst, 4, RoundingMode.HALF_UP)
				.multiply(unit)
				.multiply(mwst)
				.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
	}

	public BigDecimal getCalculateConsumption()
	{
		if (unit.compareTo(BigDecimal.ZERO) <= 0 || distance.compareTo(BigDecimal.ZERO) <= 0)
		{
			return BigDecimal.ZERO;
		}
		return unit.divide(distance, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
	}
}
