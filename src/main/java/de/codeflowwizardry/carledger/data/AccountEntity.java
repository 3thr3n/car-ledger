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

	/**
	 * Units the values are shown and entered in, stored are always km and liters
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "unit_system", nullable = false, length = 10)
	private UnitSystem unitSystem = UnitSystem.METRIC;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column(name = "last_seen")
	private OffsetDateTime lastSeen;

	/**
	 * Address for the notifications, the claim "email" of the last login
	 */
	@Column(name = "email", length = 320)
	private String email;

	/**
	 * Whether the last login had the role admin: the roles are only known from
	 * the token, the notifications for the administrators need them without one
	 */
	@Column(name = "admin", nullable = false)
	private boolean admin;

	/**
	 * Language of the browser at the last visit, EN or DE: the mails use it
	 * without a language in the settings
	 */
	@Column(name = "browser_language", length = 2)
	private String browserLanguage;

	/**
	 * Mail an administrator about new requests
	 */
	@Column(name = "notify_new_requests", nullable = false)
	private boolean notifyNewRequests = true;

	/**
	 * Mail the user when an own request was decided
	 */
	@Column(name = "notify_request_decided", nullable = false)
	private boolean notifyRequestDecided = true;

	/**
	 * Mail the reminders of the cars once they are due soon
	 */
	@Column(name = "notify_reminders", nullable = false)
	private boolean notifyReminders = true;

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

	public String getEmail()
	{
		return email;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}

	public boolean isAdmin()
	{
		return admin;
	}

	public void setAdmin(boolean admin)
	{
		this.admin = admin;
	}

	public String getBrowserLanguage()
	{
		return browserLanguage;
	}

	public void setBrowserLanguage(String browserLanguage)
	{
		this.browserLanguage = browserLanguage;
	}

	/**
	 * @return the language of the settings, else the one of the browser, else EN
	 */
	public String getMailLanguage()
	{
		if (language != null)
		{
			return language;
		}
		return browserLanguage != null ? browserLanguage : "EN";
	}

	public boolean isNotifyNewRequests()
	{
		return notifyNewRequests;
	}

	public void setNotifyNewRequests(boolean notifyNewRequests)
	{
		this.notifyNewRequests = notifyNewRequests;
	}

	public boolean isNotifyRequestDecided()
	{
		return notifyRequestDecided;
	}

	public void setNotifyRequestDecided(boolean notifyRequestDecided)
	{
		this.notifyRequestDecided = notifyRequestDecided;
	}

	public boolean isNotifyReminders()
	{
		return notifyReminders;
	}

	public void setNotifyReminders(boolean notifyReminders)
	{
		this.notifyReminders = notifyReminders;
	}

	public UnitSystem getUnitSystem()
	{
		return unitSystem;
	}

	public void setUnitSystem(UnitSystem unitSystem)
	{
		this.unitSystem = unitSystem;
	}
}
