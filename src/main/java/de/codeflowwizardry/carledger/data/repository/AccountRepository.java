package de.codeflowwizardry.carledger.data.repository;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.Account;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class AccountRepository implements PanacheRepository<Account>
{
	private final static Logger LOG = LoggerFactory.getLogger(AccountRepository.class);

	public Account findByIdentifier(String userId)
	{
		Optional<Account> singleResultOptional = find("userId", userId).singleResultOptional();
		if (singleResultOptional.isEmpty())
		{
			LOG.info("User not found! {}", userId);
			throw new BadRequestException("User cannot be found! " + userId);
		}
		return singleResultOptional.get();
	}
}
