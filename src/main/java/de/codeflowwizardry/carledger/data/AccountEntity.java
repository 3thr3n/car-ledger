package de.codeflowwizardry.carledger.data;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

@Entity(name = "Account")
public class AccountEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "user_id", nullable = false, unique = true)
	private String userId;
	@Column(name = "max_cars", nullable = false)
	private int maxCars;

	/**
	 * Default country for the VAT rate of new bills, ISO 3166 alpha-2
	 */
	@Column(name = "vat_country", nullable = false, length = 2)
	private String vatCountry = "DE";

	/**
	 * Currency amounts are shown in, ISO 4217
	 */
	@Column(name = "currency", nullable = false, length = 3)
	private String currency = "EUR";

	/**
	 * Language of the UI, <code>null</code> for the language of the browser
	 */
	@Column(name = "language", length = 2)
	private String language;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column(name = "last_seen")
	private OffsetDateTime lastSeen;

	public Long getId()
	{
		return id;
	}

	public String getUserId()
	{
		return userId;
	}

	public void setUserId(String userId)
	{
		this.userId = userId;
	}

	public int getMaxCars()
	{
		return maxCars;
	}

	public void setMaxCars(int maxCars)
	{
		this.maxCars = maxCars;
	}

	public String getVatCountry()
	{
		return vatCountry;
	}

	public void setVatCountry(String vatCountry)
	{
		this.vatCountry = vatCountry;
	}

	public String getCurrency()
	{
		return currency;
	}

	public void setCurrency(String currency)
	{
		this.currency = currency;
	}

	public String getLanguage()
	{
		return language;
	}

	public void setLanguage(String language)
	{
		this.language = language;
	}

	public OffsetDateTime getCreatedAt()
	{
		return createdAt;
	}

	public OffsetDateTime getLastSeen()
	{
		return lastSeen;
	}

	public void setLastSeen(OffsetDateTime lastSeen)
	{
		this.lastSeen = lastSeen;
	}
}
