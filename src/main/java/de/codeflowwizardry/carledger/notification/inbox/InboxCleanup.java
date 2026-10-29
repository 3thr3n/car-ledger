package de.codeflowwizardry.carledger.notification.inbox;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Deletes the notifications of the inboxes after {@link #KEEP_DAYS} days, read
 * or not: there was time to read them
 */
@ApplicationScoped
public class InboxCleanup
{
	private static final Logger LOG = LoggerFactory.getLogger(InboxCleanup.class);

	static final int KEEP_DAYS = 90;

	private final NotificationRepository repository;

	@Inject
	public InboxCleanup(NotificationRepository repository)
	{
		this.repository = repository;
	}

	@Scheduled(cron = "${carledger.notifications.cleanup-cron:0 15 3 * * ?}", identity = "inbox-cleanup", concurrentExecution = ConcurrentExecution.SKIP)
	@Transactional
	public void cleanup()
	{
		long deleted = repository.deleteOlderThan(OffsetDateTime.now().minusDays(KEEP_DAYS));
		if (deleted > 0)
		{
			LOG.info("Deleted {} notifications older than {} days", deleted, KEEP_DAYS);
		}
	}
}
