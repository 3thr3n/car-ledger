package de.codeflowwizardry.carledger.attachment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import de.codeflowwizardry.carledger.data.AttachmentEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Content as objects of a bucket of an S3 compatible storage (AWS, MinIO,
 * Garage, ...), available when a bucket is set. The client is configured by
 * <code>quarkus.s3.*</code> (endpoint, region, credentials). The objects are
 * named by a random key below the prefix, so a bucket can be shared and a
 * restored database never meets the objects of an older one.
 */
@ApplicationScoped
public class S3AttachmentStorage implements AttachmentStorage
{
	public static final String NAME = "S3";

	private final Instance<S3Client> client;
	private final Optional<String> bucket;
	private final String prefix;

	@Inject
	public S3AttachmentStorage(Instance<S3Client> client,
			@ConfigProperty(name = "carledger.attachments.s3.bucket") Optional<String> bucket,
			@ConfigProperty(name = "carledger.attachments.s3.prefix", defaultValue = "attachments/") String prefix)
	{
		this.client = client;
		this.bucket = bucket;
		this.prefix = prefix;
	}

	/**
	 * @return whether a bucket is set, files of this storage can be read
	 */
	public boolean available()
	{
		return bucket.isPresent();
	}

	@Override
	public String name()
	{
		return NAME;
	}

	@Override
	public void store(AttachmentEntity attachment, byte[] content)
	{
		String key = prefix + UUID.randomUUID();
		client.get().putObject(request -> request.bucket(bucket()).key(key)
				.contentType(attachment.getContentType())
				.contentLength((long) content.length), RequestBody.fromBytes(content));
		attachment.setStorageKey(key);
	}

	@Override
	public byte[] load(AttachmentEntity attachment)
	{
		try
		{
			return client.get().getObjectAsBytes(request -> request.bucket(bucket()).key(attachment.getStorageKey()))
					.asByteArray();
		}
		catch (NoSuchKeyException e)
		{
			throw new NotFoundException("Content of the attachment not found");
		}
	}

	@Override
	public void discard(AttachmentEntity attachment)
	{
		// the database queues the old key, deleted after the commit
	}

	/**
	 * Deleting an object that is gone already does no harm
	 */
	@Override
	public void delete(String key)
	{
		client.get().deleteObject(request -> request.bucket(bucket()).key(key));
	}

	/**
	 * Passes the keys of the objects below the prefix, one page of the listing
	 * at a time
	 *
	 * @param before
	 *            only objects stored before
	 */
	void forEachKeys(Instant before, Consumer<List<String>> keys)
	{
		client.get().listObjectsV2Paginator(request -> request.bucket(bucket()).prefix(prefix))
				.forEach(page -> keys.accept(page.contents().stream()
						.filter(object -> object.lastModified().isBefore(before))
						.map(S3Object::key)
						.toList()));
	}

	/**
	 * @throws software.amazon.awssdk.core.exception.SdkException
	 *             if the bucket cannot be reached
	 */
	void check()
	{
		client.get().headBucket(request -> request.bucket(bucket()));
	}

	private String bucket()
	{
		return bucket.orElseThrow(() -> new IllegalStateException("carledger.attachments.s3.bucket is not set"));
	}
}
