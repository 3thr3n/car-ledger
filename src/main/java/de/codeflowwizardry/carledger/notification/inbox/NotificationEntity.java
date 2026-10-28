package de.codeflowwizardry.carledger.notification.inbox;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import de.codeflowwizardry.carledger.data.AccountEntity;
import jakarta.persistence.*;

/**
 * A notification in the inbox of an account, in its language
 */
@Entity(name = "Notification")
@Table(name = "notification")
public class NotificationEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "nt_account_id", nullable = false, updatable = false)
	private AccountEntity account;

	/** the template, e.g. car-access-invited */
	@Column(name = "nt_event", nullable = false, length = 40)
	private String event;

	/** what it is about, e.g. car-request-12 */
	@Column(name = "nt_tag", nullable = false, length = 100)
	private String tag;

	@Column(name = "nt_title", nullable = false, length = 300)
	private String title;

	@Column(name = "nt_text", nullable = false)
	private String text;

	/** the page of the app it opens */
	@Column(name = "nt_path", nullable = false, length = 200)
	private String path;

	/** the values of the event, to translate it in the app later */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "nt_data")
	private Map<String, Object> data;

	@Column(name = "nt_created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);

	@Column(name = "nt_read_at")
	private OffsetDateTime readAt;

	protected NotificationEntity()
	{
	}

	public NotificationEntity(AccountEntity account, String event, String tag, String title, String text, String path,
			Map<String, Object> data)
	{
		this.account = account;
		this.event = event;
		this.tag = tag;
		this.title = title;
		this.text = text;
		this.path = path;
		this.data = data;
	}

	public Long getId()
	{
		return id;
	}

	public AccountEntity getAccount()
	{
		return account;
	}

	public String getEvent()
	{
		return event;
	}

	public String getTag()
	{
		return tag;
	}

	public String getTitle()
	{
		return title;
	}

	public String getText()
	{
		return text;
	}

	public String getPath()
	{
		return path;
	}

	public Map<String, Object> getData()
	{
		return data;
	}

	public OffsetDateTime getCreatedAt()
	{
		return createdAt;
	}

	public OffsetDateTime getReadAt()
	{
		return readAt;
	}

	public void markRead()
	{
		if (readAt == null)
		{
			readAt = OffsetDateTime.now();
		}
	}

	/**
	 * Another text for the same tag, e.g. a request another administrator decided
	 */
	public void replace(String event, String title, String text, Map<String, Object> data)
	{
		this.event = event;
		this.title = title;
		this.text = text;
		this.data = data;
	}
}
