package de.codeflowwizardry.carledger.request;

import java.util.List;

import de.codeflowwizardry.carledger.Utils;
import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.exception.AlreadyPresentException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

/**
 * The life of a request: approved by the handler of its type, or rejected; the
 * user sees the outcome once
 */
@ApplicationScoped
public class RequestService
{
	public static final int MAX_TEXT = 500;

	private final RequestRepository repository;

	@Inject
	public RequestService(RequestRepository repository)
	{
		this.repository = repository;
	}

	/**
	 * @throws NotFoundException
	 *             if there is no request of the type with the id
	 * @throws AlreadyPresentException
	 *             if the request is decided already
	 */
	@Transactional
	public <I> RequestEntity approve(long id, RequestHandler<I> handler, I input, String decidedBy)
	{
		RequestEntity request = findOpen(id, handler.type());
		handler.approve(request, input);
		request.decide(RequestState.APPROVED, decidedBy, null);
		return request;
	}

	/**
	 * @param comment
	 *            optional, shown to the user
	 */
	@Transactional
	public RequestEntity reject(long id, RequestType type, String comment, String decidedBy)
	{
		Utils.requireValid(comment != null && comment.length() > MAX_TEXT
				? List.of("The comment can have at most " + MAX_TEXT + " characters!")
				: List.of());
		RequestEntity request = findOpen(id, type);
		request.decide(RequestState.REJECTED, decidedBy, comment == null || comment.isBlank() ? null : comment.trim());
		return request;
	}

	/**
	 * The user has seen the decision, it is not shown again
	 */
	@Transactional
	public void markSeen(long id, AccountEntity account)
	{
		RequestEntity request = repository.findByIdOptional(id)
				.filter(found -> found.getAccount().getId().equals(account.getId()))
				.orElseThrow(() -> new NotFoundException("Request not found"));
		if (request.getState() != RequestState.OPEN)
		{
			request.setOutcomeSeen(true);
		}
	}

	private RequestEntity findOpen(long id, RequestType type)
	{
		RequestEntity request = repository.findByIdOptional(id)
				.filter(found -> found.getType() == type)
				.orElseThrow(() -> new NotFoundException("Request not found"));
		if (request.getState() != RequestState.OPEN)
		{
			throw new AlreadyPresentException("The request was decided already!");
		}
		return request;
	}
}
