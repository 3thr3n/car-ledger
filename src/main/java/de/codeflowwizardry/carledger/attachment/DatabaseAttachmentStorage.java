package de.codeflowwizardry.carledger.attachment;

import de.codeflowwizardry.carledger.data.AttachmentContentEntity;
import de.codeflowwizardry.carledger.data.AttachmentEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.NotFoundException;

/**
 * Content in the table <code>attachment_content</code>, deleted by the database
 * with the attachment, its bill or its car
 */
@ApplicationScoped
public class DatabaseAttachmentStorage implements AttachmentStorage
{
	public static final String NAME = "DB";

	private final EntityManager entityManager;

	@Inject
	public DatabaseAttachmentStorage(EntityManager entityManager)
	{
		this.entityManager = entityManager;
	}

	@Override
	public String name()
	{
		return NAME;
	}

	@Override
	public void store(AttachmentEntity attachment, byte[] content)
	{
		entityManager.persist(new AttachmentContentEntity(attachment.getId(), content));
	}

	@Override
	public byte[] load(AttachmentEntity attachment)
	{
		AttachmentContentEntity content = entityManager.find(AttachmentContentEntity.class, attachment.getId());
		if (content == null)
		{
			throw new NotFoundException("Content of the attachment not found");
		}
		return content.getData();
	}

	@Override
	public void delete(AttachmentEntity attachment)
	{
		// removed by the foreign key with the attachment
	}
}
