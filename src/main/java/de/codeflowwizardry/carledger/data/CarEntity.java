package de.codeflowwizardry.carledger.data;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity(name = "Car")
public class CarEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "c_name", nullable = false)
	private String name;
	@Column(name = "c_manufacture_year", nullable = false)
	private int manufactureYear;
	@Column(name = "c_odometer", nullable = false)
	private int odometer = 0;
	/** day of the odometer reading, refuels after it are added to it */
	@Column(name = "c_odometer_date", nullable = false)
	private LocalDate odometerDate = LocalDate.now();
	@Column(name = "c_license_plate")
	private String licensePlate;
	@Column(name = "c_vin")
	private String vin;
	@Column(name = "c_purchase_date")
	private LocalDate purchaseDate;
	@Column(name = "c_purchase_price")
	private BigDecimal purchasePrice;
	@Enumerated(EnumType.STRING)
	@Column(name = "c_energy_type", nullable = false)
	private EnergyType energyType = EnergyType.PETROL;

	/** in liters, only for cars with a tank */
	@Column(name = "c_tank_size")
	private BigDecimal tankSize;

	/** in kWh, only for cars with a battery */
	@Column(name = "c_battery_capacity")
	private BigDecimal batteryCapacity;

	/** the trip log (Fahrtenbuch) is used: trips can be started */
	@Column(name = "c_trip_log", nullable = false)
	private boolean tripLog;

	/** attachment with the picture, the generated avatar is shown without */
	@Column(name = "c_picture_id")
	private Long pictureId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "c_user_id", nullable = false)
	private AccountEntity user;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public int getManufactureYear()
	{
		return manufactureYear;
	}

	public void setManufactureYear(int manufactureYear)
	{
		this.manufactureYear = manufactureYear;
	}

	public int getOdometer()
	{
		return odometer;
	}

	public void setOdometer(int odometer)
	{
		this.odometer = odometer;
	}

	public Long getId()
	{
		return id;
	}

	public AccountEntity getUser()
	{
		return user;
	}

	public void setUser(AccountEntity user)
	{
		this.user = user;
	}

	public LocalDate getOdometerDate()
	{
		return odometerDate;
	}

	public void setOdometerDate(LocalDate odometerDate)
	{
		this.odometerDate = odometerDate;
	}

	public String getLicensePlate()
	{
		return licensePlate;
	}

	public void setLicensePlate(String licensePlate)
	{
		this.licensePlate = licensePlate;
	}

	public String getVin()
	{
		return vin;
	}

	public void setVin(String vin)
	{
		this.vin = vin;
	}

	public LocalDate getPurchaseDate()
	{
		return purchaseDate;
	}

	public void setPurchaseDate(LocalDate purchaseDate)
	{
		this.purchaseDate = purchaseDate;
	}

	public BigDecimal getPurchasePrice()
	{
		return purchasePrice;
	}

	public void setPurchasePrice(BigDecimal purchasePrice)
	{
		this.purchasePrice = purchasePrice;
	}

	public EnergyType getEnergyType()
	{
		return energyType;
	}

	public void setEnergyType(EnergyType energyType)
	{
		this.energyType = energyType;
	}

	public BigDecimal getTankSize()
	{
		return tankSize;
	}

	public void setTankSize(BigDecimal tankSize)
	{
		this.tankSize = tankSize;
	}

	public BigDecimal getBatteryCapacity()
	{
		return batteryCapacity;
	}

	public void setBatteryCapacity(BigDecimal batteryCapacity)
	{
		this.batteryCapacity = batteryCapacity;
	}

	public boolean isTripLog()
	{
		return tripLog;
	}

	public void setTripLog(boolean tripLog)
	{
		this.tripLog = tripLog;
	}

	public Long getPictureId()
	{
		return pictureId;
	}

	public void setPictureId(Long pictureId)
	{
		this.pictureId = pictureId;
	}
}
