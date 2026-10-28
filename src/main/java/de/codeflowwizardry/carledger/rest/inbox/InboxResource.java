package de.codeflowwizardry.carledger.rest.inbox;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.codeflowwizardry.carledger.data.AccountEntity;
import de.codeflowwizardry.carledger.data.repository.AccountRepository;
import de.codeflowwizardry.carledger.notification.inbox.NotificationEntity;
import de.codeflowwizardry.carledger.notification.inbox.NotificationRepository;
import de.codeflowwizardry.carledger.rest.AbstractResource;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * The inbox of the own account: every notification the server sent it, also
 * with mail and push off, for 90 days
 */
@Path("inbox")
@Produces(MediaType.APPLICATION_JSON)
public class InboxResource extends AbstractResource
{
	private final NotificationRepository repository;

	@Inject
	public InboxResource(SecurityIdentity identity, AccountRepository accountRepository,
			NotificationRepository repository)
	{
		super(identity, accountRepository);
		this.repository = repository;
	}

	@GET
	@Operation(operationId = "getInbox", description = "The notifications of the own account, newest first; pages starting at 1")
	@APIResponse(responseCode = "200", description = "Notifications found.")
	public InboxPage getInbox(@QueryParam("page") @DefaultValue("1") int page,
			@QueryParam("size") @DefaultValue("20") int size)
	{
		page = toPageNumber(page);
		size = toPageSize(size);
		AccountEntity account = getAccount();
		return new InboxPage(repository.count(account), repository.countUnread(account), page, size,
				repository.list(account, page - 1, size).stream().map(InboxEntry::convert).toList());
	}

	@GET
	@Path("unread")
	@Operation(operationId = "getInboxUnread", description = "How many notifications of the own account are unread")
	@APIResponse(responseCode = "200", description = "Counted.")
	public InboxUnread getUnread()
	{
		return new InboxUnread(repository.countUnread(getAccount()));
	}

	@POST
	@Path("{id}/read")
	@Transactional
	@Operation(operationId = "readInboxEntry", description = "Marks a notification as read, e.g. when its page was opened")
	@APIResponse(responseCode = "204", description = "Marked.")
	@APIResponse(responseCode = "404", description = "Not a notification of the own account.")
	public void read(@PathParam("id") long id)
	{
		find(id).markRead();
	}

	@POST
	@Path("read")
	@Transactional
	@Operation(operationId = "readInbox", description = "Marks all notifications of the own account as read")
	@APIResponse(responseCode = "204", description = "Marked.")
	public void readAll()
	{
		repository.markAllRead(getAccount());
	}

	@DELETE
	@Path("{id}")
	@Transactional
	@Operation(operationId = "deleteInboxEntry", description = "Deletes a notification of the own account")
	@APIResponse(responseCode = "204", description = "Deleted.")
	@APIResponse(responseCode = "404", description = "Not a notification of the own account.")
	public void delete(@PathParam("id") long id)
	{
		repository.delete(find(id));
	}

	private NotificationEntity find(long id)
	{
		return repository.find(getAccount(), id).orElseThrow(() -> new NotFoundException("Notification not found"));
	}
}
