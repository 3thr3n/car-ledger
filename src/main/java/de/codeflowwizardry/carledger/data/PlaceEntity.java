package de.codeflowwizardry.carledger.data;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;

/**
 * A place the owner of the car named, e.g. a fuel station it stops at often
 */
@Entity(name = "place")
public class PlaceEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(insertable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pl_car_id", nullable = false, updatable = false)
	private CarEntity car;

	@Column(name = "pl_lat", nullable = false)
	private double lat;

	@Column(name = "pl_lon", nullable = false)
	private double lon;

	@Column(name = "pl_name", nullable = false, length = 100)
	private String name;

	@Column(name = "pl_created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

	protected PlaceEntity()
	{
	}

	public PlaceEntity(CarEntity car, double lat, double lon, String name)
	{
		this.car = car;
		this.lat = lat;
		this.lon = lon;
		this.name = name;
	}

	public Long getId()
	{
		return id;
	}

	public CarEntity getCar()
	{
		return car;
	}

	public double getLat()
	{
		return lat;
	}

	public double getLon()
	{
		return lon;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
}
