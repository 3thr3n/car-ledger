package de.codeflowwizardry.carledger.notification.push;

import java.time.OffsetDateTime;

import de.codeflowwizardry.carledger.data.AccountEntity;
import jakarta.persistence.*;

/**
 * A browser that receives the notifications of an account
 */
@Entity(name = "PushSubscription")
@Table(name = "push_subscription")
public class PushSubscriptionEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ps_account_id", nullable = false)
	private AccountEntity account;

	/** URL of the push service for this browser, unique */
	@Column(name = "ps_endpoint", nullable = false, length = 2048)
	private String endpoint;

	/** public key of the browser, base64url */
	@Column(name = "ps_p256dh", nullable = false, length = 100)
	private String p256dh;

	/** secret of the browser, base64url */
	@Column(name = "ps_auth", nullable = false, length = 50)
	private String auth;

	@Column(name = "ps_created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	protected PushSubscriptionEntity()
	{
	}

	public PushSubscriptionEntity(String endpoint)
	{
		this.endpoint = endpoint;
	}

	public Long getId()
	{
		return id;
	}

	public AccountEntity getAccount()
	{
		return account;
	}

	public void setAccount(AccountEntity account)
	{
		this.account = account;
	}

	public String getEndpoint()
	{
		return endpoint;
	}

	public String getP256dh()
	{
		return p256dh;
	}

	public void setP256dh(String p256dh)
	{
		this.p256dh = p256dh;
	}

	public String getAuth()
	{
		return auth;
	}

	public void setAuth(String auth)
	{
		this.auth = auth;
	}

	public OffsetDateTime getCreatedAt()
	{
		return createdAt;
	}
}
