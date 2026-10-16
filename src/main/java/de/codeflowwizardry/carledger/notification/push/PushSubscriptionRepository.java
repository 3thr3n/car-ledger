package de.codeflowwizardry.carledger.notification.push;

import java.util.List;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.AccountEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PushSubscriptionRepository implements PanacheRepository<PushSubscriptionEntity>
{
	public List<PushSubscriptionEntity> listFor(AccountEntity account)
	{
		return list("account", account);
	}

	public Optional<PushSubscriptionEntity> findByEndpoint(String endpoint)
	{
		return find("endpoint", endpoint).firstResultOptional();
	}

	public boolean hasAny(AccountEntity account)
	{
		return count("account", account) > 0;
	}
}
