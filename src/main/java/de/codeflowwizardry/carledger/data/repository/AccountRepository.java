package de.codeflowwizardry.carledger.data.repository;

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
}
