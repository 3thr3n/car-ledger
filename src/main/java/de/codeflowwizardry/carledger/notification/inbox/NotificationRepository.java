package de.codeflowwizardry.carledger.notification.inbox;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.AccountEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NotificationRepository implements PanacheRepository<NotificationEntity>
{
	/**
	 * @param page
	 *            starts at 0
	 * @return the newest first
	 */
	public List<NotificationEntity> list(AccountEntity account, int page, int size)
	{
		return find("account", Sort.by("createdAt", Sort.Direction.Descending).and("id", Sort.Direction.Descending),
				account).page(Page.of(page, size)).list();
	}

	public long count(AccountEntity account)
	{
		return count("account", account);
	}

	public long countUnread(AccountEntity account)
	{
		return count("account = ?1 and readAt is null", account);
	}

	public Optional<NotificationEntity> find(AccountEntity account, long id)
	{
		return find("account = ?1 and id = ?2", account, id).firstResultOptional();
	}

	/**
	 * @return the amount marked
	 */
	public int markAllRead(AccountEntity account)
	{
		return update("readAt = ?1 where account = ?2 and readAt is null", OffsetDateTime.now(), account);
	}

	/**
	 * @return of all accounts, e.g. of the administrators about one request
	 */
	public List<NotificationEntity> listByTag(String event, String tag)
	{
		return list("event = ?1 and tag = ?2", event, tag);
	}

	/**
	 * @return the amount deleted
	 */
	public long deleteOlderThan(OffsetDateTime before)
	{
		return delete("createdAt < ?1", before);
	}
}
