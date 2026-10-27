package de.codeflowwizardry.carledger.attachment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * The storages of this installation: new files go to the configured one
 * (<code>carledger.attachments.storage</code>), the files of every available
 * one stay readable, so switching the storage keeps the existing files.
 */
@ApplicationScoped
public class AttachmentStorages
{
	private static final Logger LOG = LoggerFactory.getLogger(AttachmentStorages.class);

	private final Map<String, AttachmentStorage> available = new LinkedHashMap<>();
	private final AttachmentStorage configured;
	private final S3AttachmentStorage s3;

	@Inject
	public AttachmentStorages(DatabaseAttachmentStorage database, S3AttachmentStorage s3,
			@ConfigProperty(name = "carledger.attachments.storage", defaultValue = DatabaseAttachmentStorage.NAME) String storage)
	{
		this.s3 = s3;
		available.put(database.name(), database);
		if (s3.available())
		{
			available.put(s3.name(), s3);
		}
		configured = find(storage.toUpperCase()).orElseThrow(() -> new IllegalStateException(
				"The attachment storage " + storage + " is not available, " + DatabaseAttachmentStorage.NAME
						+ " or " + S3AttachmentStorage.NAME + " with carledger.attachments.s3.bucket"));
	}

	/**
	 * Only logs a bucket that cannot be reached: the app keeps running, the
	 * files of the database stay usable
	 */
	void check(@Observes StartupEvent event)
	{
		if (!s3.available())
		{
			return;
		}
		try
		{
			s3.check();
		}
		catch (RuntimeException e)
		{
			LOG.error("The bucket of the attachments cannot be reached: {}", e.getMessage());
		}
	}

	/**
	 * @return the storage of new files
	 */
	public AttachmentStorage configured()
	{
		return configured;
	}

	public Optional<AttachmentStorage> find(String name)
	{
		return Optional.ofNullable(available.get(name));
	}

	/**
	 * @throws IllegalStateException
	 *             if the storage isn't available, e.g. S3 without a bucket
	 */
	public AttachmentStorage get(String name)
	{
		return find(name).orElseThrow(() -> new IllegalStateException("Storage " + name + " is not configured"));
	}

	S3AttachmentStorage s3()
	{
		return s3;
	}
}
