package de.codeflowwizardry.carledger.attachment;

import de.codeflowwizardry.carledger.data.AttachmentEntity;

/**
 * Keeps the content of attachments. The name is saved with every attachment,
 * so the content can be found again after another storage was configured.
 */
public interface AttachmentStorage
{
	/**
	 * @return name saved in {@link AttachmentEntity#getStorage()}
	 */
	String name();

	/**
	 * Stores the content of the persisted attachment, may set its storage key
	 */
	void store(AttachmentEntity attachment, byte[] content);

	byte[] load(AttachmentEntity attachment);

	/**
	 * Stores the preview next to the stored content of the attachment, replaces
	 * an existing one
	 */
	void storePreview(AttachmentEntity attachment, byte[] preview);

	/**
	 * @throws jakarta.ws.rs.NotFoundException
	 *             if no preview is stored
	 */
	byte[] loadPreview(AttachmentEntity attachment);

	/**
	 * Removes the content and the preview of an attachment that moves to another
	 * storage, in its transaction. Storages outside the database are cleaned up by
	 * {@link #delete(String)} after the commit instead.
	 */
	void discard(AttachmentEntity attachment);

	/**
	 * Removes the content and the preview of a deleted or moved attachment,
	 * queued with its key in <code>attachment_removal</code>. Storages in the
	 * database are cleaned up
	 * with the attachment by the database itself and have no keys.
	 */
	void delete(String key);
}
