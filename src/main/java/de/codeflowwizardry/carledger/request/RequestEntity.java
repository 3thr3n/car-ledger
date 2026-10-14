package de.codeflowwizardry.carledger.request;

import java.time.OffsetDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import de.codeflowwizardry.carledger.data.AccountEntity;
import jakarta.persistence.*;

/**
 * A request of a user that an administrator approves or rejects. The payload
 * depends on the {@link RequestType}.
 */
@Entity(name = "Request")
@Table(name = "request")
public class RequestEntity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rq_account_id", nullable = false)
	private AccountEntity account;

	@Enumerated(EnumType.STRING)
	@Column(name = "rq_type", nullable = false)
	private RequestType type;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "rq_payload", nullable = false)
	private Map<String, Object> payload;

	@Column(name = "rq_reason")
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(name = "rq_state", nullable = false)
	private RequestState state = RequestState.OPEN;

	@Column(name = "rq_created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column(name = "rq_decided_at")
	private OffsetDateTime decidedAt;

	@Column(name = "rq_decided_by")
	private String decidedBy;

	@Column(name = "rq_comment")
	private String comment;

	@Column(name = "rq_outcome_seen", nullable = false)
	private boolean outcomeSeen;

	protected RequestEntity()
	{
	}

	public RequestEntity(AccountEntity account, RequestType type, Map<String, Object> payload, String reason)
	{
		this.account = account;
		this.type = type;
		this.payload = payload;
		this.reason = reason;
	}

	/**
	 * Closes the request
	 *
	 * @param decidedBy
	 *            user name of the administrator
	 */
	public void decide(RequestState state, String decidedBy, String comment)
	{
		this.state = state;
		this.decidedBy = decidedBy;
		this.comment = comment;
		this.decidedAt = OffsetDateTime.now();
	}

	public Long getId()
	{
		return id;
	}

	public AccountEntity getAccount()
	{
		return account;
	}

	public RequestType getType()
	{
		return type;
	}

	public Map<String, Object> getPayload()
	{
		return payload;
	}

	public void setPayload(Map<String, Object> payload)
	{
		this.payload = payload;
	}

	public String getReason()
	{
		return reason;
	}

	public RequestState getState()
	{
		return state;
	}

	public OffsetDateTime getCreatedAt()
	{
		return createdAt;
	}

	public OffsetDateTime getDecidedAt()
	{
		return decidedAt;
	}

	public String getDecidedBy()
	{
		return decidedBy;
	}

	public String getComment()
	{
		return comment;
	}

	public boolean isOutcomeSeen()
	{
		return outcomeSeen;
	}

	public void setOutcomeSeen(boolean outcomeSeen)
	{
		this.outcomeSeen = outcomeSeen;
	}
}
