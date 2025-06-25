package de.codeflowwizardry.carledger.data.repository;

import de.codeflowwizardry.carledger.data.Account;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class AccountRepository implements PanacheRepository<Account>
{
	public Optional<Account> findByIdentifierOptional(String userId)
	{
		return find("userId", userId).singleResultOptional();
	}

	public Account findByIdentifier(String userId)
	{
		return find("userId", userId).singleResult();
	}
}
