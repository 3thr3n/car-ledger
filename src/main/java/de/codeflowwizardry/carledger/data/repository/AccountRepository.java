package de.codeflowwizardry.carledger.data.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.AccountEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class AccountRepository implements PanacheRepository<AccountEntity>
{
	private final static Logger LOG = LoggerFactory.getLogger(AccountRepository.class);

	public AccountEntity findByIdentifier(String userId)
	{
		Optional<AccountEntity> singleResultOptional = find("userId", userId).singleResultOptional();
		if (singleResultOptional.isEmpty())
		{
			LOG.info("User not found! {}", userId);
			throw new BadRequestException("User cannot be found! " + userId);
		}
		return singleResultOptional.get();
	}

	/**
	 * @param name
	 *            user name or e-mail address in lower case; an address finds only
	 *            an account whose identity provider confirmed it
	 */
	public Optional<AccountEntity> findByNameOrEmail(String name)
	{
		return find("lower(userId) = ?1", name).firstResultOptional()
				.or(() -> find("lower(email) = ?1 and emailVerified = true", name).firstResultOptional());
	}

	/**
	 * Creates the account in its own transaction and flushes immediately, so a
	 * concurrent insert of the same user
	 * fails here with a
	 * {@link org.hibernate.exception.ConstraintViolationException} instead of at
	 * the commit of the
	 * callers transaction.
	 */
	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public AccountEntity createAccount(String userId, int maxCars)
	{
		AccountEntity accountEntity = new AccountEntity();
		accountEntity.setUserId(userId);
		accountEntity.setMaxCars(maxCars);
		persistAndFlush(accountEntity);
		return accountEntity;
	}

	/**
	 * @param search
	 *            part of the user name, case insensitive, <code>null</code> for all
	 * @return accounts with the amount of their cars and bills, ordered by user
	 *         name
	 */
	public List<AccountUsage> listUsage(String search, int page, int size)
	{
		return getEntityManager()
				.createQuery("select a,"
						+ " (select count(c) from Car c where c.user = a),"
						+ " (select count(b) from bill b where b.car.user = a)"
						+ " from Account a where " + SEARCH + " order by a.userId", Object[].class)
				.setParameter("search", searchPattern(search))
				.setFirstResult(page * size)
				.setMaxResults(size)
				.getResultList()
				.stream()
				.map(row -> new AccountUsage((AccountEntity) row[0], (Long) row[1], (Long) row[2]))
				.toList();
	}

	public long countMatching(String search)
	{
		return getEntityManager()
				.createQuery("select count(a) from Account a where " + SEARCH, Long.class)
				.setParameter("search", searchPattern(search))
				.getSingleResult();
	}

	/**
	 * @return amount of accounts seen since the given time
	 */
	public long countSeenSince(OffsetDateTime since)
	{
		return count("lastSeen >= ?1", since);
	}

	/**
	 * Deletes the account, the database removes its cars and their bills as well
	 *
	 * @return whether the account existed
	 */
	@Transactional
	public boolean deleteAccount(long id)
	{
		return delete("id", id) == 1;
	}

	private static final String SEARCH = "lower(a.userId) like :search";

	private static String searchPattern(String search)
	{
		return "%" + (search == null ? "" : search.toLowerCase().replace("%", "").replace("_", "")) + "%";
	}

	public record AccountUsage(AccountEntity account, long cars, long bills)
	{
	}
}
