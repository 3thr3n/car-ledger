package de.codeflowwizardry.carledger.request;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.codeflowwizardry.carledger.data.AccountEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RequestRepository implements PanacheRepository<RequestEntity>
{
	/**
	 * @return the newest request of the account and type
	 */
	public Optional<RequestEntity> latest(AccountEntity account, RequestType type)
	{
		return find("account = ?1 and type = ?2", Sort.descending("createdAt", "id"), account, type)
				.firstResultOptional();
	}

	public boolean hasOpen(AccountEntity account, RequestType type)
	{
		return count("account = ?1 and type = ?2 and state = ?3", account, type, RequestState.OPEN) > 0;
	}

	/**
	 * @param state
	 *            all states without one
	 * @param page
	 *            starts at 0
	 * @return the open ones by age, the decided ones newest first; with the account
	 */
	public List<RequestEntity> list(RequestType type, RequestState state, int page, int size)
	{
		String query = "from Request r join fetch r.account where r.type = :type"
				+ (state == null ? "" : " and r.state = :state");
		Sort sort = state == RequestState.OPEN
				? Sort.ascending("r.createdAt", "r.id")
				: Sort.descending("r.createdAt", "r.id");
		return find(query, sort, parameters(type, state)).page(Page.of(page, size)).list();
	}

	public long count(RequestType type, RequestState state)
	{
		return count("type = :type" + (state == null ? "" : " and state = :state"), parameters(type, state));
	}

	private static Map<String, Object> parameters(RequestType type, RequestState state)
	{
		return state == null ? Map.of("type", type) : Map.of("type", type, "state", state);
	}
}
