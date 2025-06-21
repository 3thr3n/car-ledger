package de.codeflowwizardry.carledger.data.repository;

import de.codeflowwizardry.carledger.data.Account;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@ApplicationScoped
public class AccountRepository implements PanacheRepository<Account>
{
	private final static Logger LOG = LoggerFactory.getLogger(AccountRepository.class);

	public Optional<Account> findByIdentifier(String userId)
	{
        return find("userId", userId).singleResultOptional();
	}
}
