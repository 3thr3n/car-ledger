package de.codeflowwizardry.carledger.attachment;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.codeflowwizardry.carledger.data.AttachmentEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Moves the files of the other storages into the configured one, when
 * <code>carledger.attachments.move</code> is set, e.g. after switching from
 * the database to S3. A file per transaction: a restart continues with the
 * files left, several instances move different files. The content left in the
 * old storage is deleted with the queue of {@link AttachmentCleanup}.
 */
@ApplicationScoped
public class AttachmentMover
{
	private static final Logger LOG = LoggerFactory.getLogger(AttachmentMover.class);

	private final AttachmentStorages storages;
	private final EntityManager entityManager;
	private final boolean enabled;

	@Inject
	public AttachmentMover(AttachmentStorages storages, EntityManager entityManager,
			@ConfigProperty(name = "carledger.attachments.move", defaultValue = "false") boolean enabled)
	{
		this.storages = storages;
		this.entityManager = entityManager;
		this.enabled = enabled;
	}

	/**
	 * @param moved
	 *            files now in the configured storage
	 * @param failed
	 *            files that could not be moved, tried again with the next run
	 * @param left
	 *            files still in other storages, e.g. moved by another instance
	 *            at the moment or failed
	 */
	public record Summary(int moved, List<Long> failed, long left)
	{
	}

	@Scheduled(every = "${carledger.attachments.move-every:1h}", delayed = "1m", concurrentExecution = ConcurrentExecution.SKIP)
	void scheduled()
	{
		if (enabled)
		{
			move();
		}
	}

	/**
	 * Moves all files of the other storages into the configured one and logs a
	 * summary
	 */
	public Summary move()
	{
		AttachmentStorage target = storages.configured();
		int moved = 0;
		List<Long> failed = new ArrayList<>();
		while (true)
		{
			try
			{
				if (!QuarkusTransaction.requiringNew().call(() -> moveNext(target, failed)))
				{
					break;
				}
				moved++;
			}
			catch (MoveFailed e)
			{
				failed.add(e.id);
			}
		}

		long left = QuarkusTransaction.requiringNew().call(() -> ((Number) entityManager
				.createNativeQuery("SELECT count(*) FROM attachment WHERE a_storage <> :target")
				.setParameter("target", target.name())
				.getSingleResult()).longValue());
		Summary summary = new Summary(moved, failed, left);
		if (moved > 0 || !failed.isEmpty())
		{
			LOG.info("Moved {} attachments to the storage {}, {} failed {}, {} left in other storages", moved,
					target.name(), failed.size(), failed, left);
		}
		return summary;
	}

	/**
	 * rolls the transaction of the file back
	 */
	private static class MoveFailed extends RuntimeException
	{
		private final long id;

		MoveFailed(long id, Throwable cause)
		{
			super(cause);
			this.id = id;
		}
	}

	/**
	 * @param failed
	 *            skipped in this run
	 * @return whether a file was moved, false when all are moved
	 * @throws MoveFailed
	 *             if the file taken could not be moved
	 */
	private boolean moveNext(AttachmentStorage target, List<Long> failed)
	{
		@SuppressWarnings("unchecked")
		List<Number> ids = entityManager.createNativeQuery(
				"SELECT id FROM attachment WHERE a_storage <> :target AND NOT (id = ANY (:failed))"
						+ " ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED")
				.setParameter("target", target.name())
				.setParameter("failed", failed.toArray(Long[]::new))
				.getResultList();
		if (ids.isEmpty())
		{
			return false;
		}

		AttachmentEntity attachment = entityManager.find(AttachmentEntity.class, ids.getFirst().longValue());
		String sourceName = attachment.getStorage();
		try
		{
			AttachmentStorage source = storages.get(sourceName);
			byte[] content = source.load(attachment);
			source.discard(attachment);

			// the database queues the old key for the removal
			attachment.setStorage(target.name());
			attachment.setStorageKey(null);
			target.store(attachment, content);
			// generated again in the new storage on the next request
			attachment.setPreview(null);
			entityManager.flush();
			return true;
		}
		catch (RuntimeException e)
		{
			LOG.error("Could not move the attachment {} from the storage {} to {}", attachment.getId(), sourceName,
					target.name(), e);
			throw new MoveFailed(attachment.getId(), e);
		}
	}
}
