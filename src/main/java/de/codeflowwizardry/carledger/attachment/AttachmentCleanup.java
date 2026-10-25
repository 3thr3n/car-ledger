package de.codeflowwizardry.carledger.attachment;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Deletes the content of attachments outside the database, which the foreign
 * keys don't reach. The database queues the key of every deleted or moved
 * attachment in <code>attachment_removal</code> (V1.26.0), whatever deleted it:
 * the attachment, its bill, modification, inspection, car or account. Every
 * instance runs the jobs, a removal is taken by one of them.
 */
@ApplicationScoped
public class AttachmentCleanup
{
	private static final Logger LOG = LoggerFactory.getLogger(AttachmentCleanup.class);

	/** removals per transaction */
	private static final int BATCH = 100;
	/**
	 * an object without an attachment this old is an orphan, e.g. of an upload
	 * whose transaction was rolled back; younger ones may belong to a
	 * transaction still running (a restore takes up to its timeout)
	 */
	static final Duration ORPHAN_AGE = Duration.ofDays(1);

	private final AttachmentStorages storages;
	private final EntityManager entityManager;

	@Inject
	public AttachmentCleanup(AttachmentStorages storages, EntityManager entityManager)
	{
		this.storages = storages;
		this.entityManager = entityManager;
	}

	@Scheduled(every = "${carledger.attachments.cleanup-every:1m}", delayed = "1m", concurrentExecution = ConcurrentExecution.SKIP)
	void removeQueued()
	{
		removeQueued(BATCH);
	}

	/**
	 * Deletes the content of the queued removals, a batch per transaction. A
	 * removal of a storage that isn't available (anymore) or fails stays queued
	 * for the next run.
	 *
	 * @return the amount of deleted contents
	 */
	int removeQueued(int batch)
	{
		int removed = 0;
		Set<Long> kept = new HashSet<>();
		while (true)
		{
			int[] counts = QuarkusTransaction.requiringNew().call(() -> removeBatch(batch, kept));
			removed += counts[0];
			if (counts[1] < batch)
			{
				break;
			}
		}
		if (removed > 0)
		{
			LOG.info("Deleted the content of {} removed attachments", removed);
		}
		return removed;
	}

	/**
	 * @param kept
	 *            removals that failed in this run, skipped until the next
	 * @return the amount of removed contents and of the removals taken
	 */
	private int[] removeBatch(int batch, Set<Long> kept)
	{
		@SuppressWarnings("unchecked")
		List<Object[]> rows = entityManager.createNativeQuery(
				"SELECT id, ar_storage, ar_storage_key FROM attachment_removal WHERE NOT (id = ANY (:kept))"
						+ " ORDER BY id LIMIT :batch FOR UPDATE SKIP LOCKED")
				.setParameter("kept", kept.toArray(Long[]::new))
				.setParameter("batch", batch)
				.getResultList();

		int removed = 0;
		for (Object[] row : rows)
		{
			long id = ((Number) row[0]).longValue();
			String storageName = (String) row[1];
			String key = (String) row[2];
			Optional<AttachmentStorage> storage = storages.find(storageName);
			if (storage.isEmpty())
			{
				LOG.warn("The content {} of the storage {} cannot be deleted, it isn't configured", key, storageName);
				kept.add(id);
				continue;
			}
			try
			{
				storage.get().delete(key);
			}
			catch (RuntimeException e)
			{
				LOG.error("Could not delete the content {} of the storage {}", key, storageName, e);
				kept.add(id);
				continue;
			}
			entityManager.createNativeQuery("DELETE FROM attachment_removal WHERE id = :id")
					.setParameter("id", id)
					.executeUpdate();
			removed++;
		}
		return new int[] {
				removed, rows.size()
		};
	}

	@Scheduled(cron = "${carledger.attachments.orphans-cron:0 45 2 * * ?}", concurrentExecution = ConcurrentExecution.SKIP)
	void removeOrphans()
	{
		removeOrphans(Instant.now());
	}

	/**
	 * Deletes the objects of the S3 storage older than {@link #ORPHAN_AGE} that
	 * no attachment or queued removal knows
	 *
	 * @return the amount of deleted objects
	 */
	int removeOrphans(Instant now)
	{
		S3AttachmentStorage s3 = storages.s3();
		if (!s3.available())
		{
			return 0;
		}
		int[] removed = {
				0
		};
		try
		{
			s3.forEachKeys(now.minus(ORPHAN_AGE), keys -> {
				if (keys.isEmpty())
				{
					return;
				}
				Set<String> known = QuarkusTransaction.requiringNew().call(() -> known(keys));
				for (String key : keys)
				{
					if (!known.contains(key))
					{
						s3.delete(key);
						removed[0]++;
					}
				}
			});
		}
		catch (RuntimeException e)
		{
			LOG.error("Could not delete the orphaned objects of the attachments", e);
		}
		if (removed[0] > 0)
		{
			LOG.info("Deleted {} objects of the attachments without an attachment", removed[0]);
		}
		return removed[0];
	}

	/**
	 * @return the keys an attachment or a queued removal (deleted by
	 *         {@link #removeQueued} anyway) has
	 */
	@SuppressWarnings("unchecked")
	private Set<String> known(List<String> keys)
	{
		return new HashSet<>(entityManager.createNativeQuery(
				"SELECT a_storage_key FROM attachment WHERE a_storage_key IN (:keys)"
						+ " UNION SELECT ar_storage_key FROM attachment_removal WHERE ar_storage_key IN (:keys)")
				.setParameter("keys", keys)
				.getResultList());
	}
}
