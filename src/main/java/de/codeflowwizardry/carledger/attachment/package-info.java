/**
 * The files of the entries, the documents of modifications and HUs, and the car
 * pictures.
 * <p>
 * {@link de.codeflowwizardry.carledger.attachment.AttachmentService} checks the
 * access, the limits and the type
 * ({@link de.codeflowwizardry.carledger.attachment.FileType}) and keeps the
 * metadata in PostgreSQL. The content goes to the
 * {@link de.codeflowwizardry.carledger.attachment.AttachmentStorage} chosen by
 * {@link de.codeflowwizardry.carledger.attachment.AttachmentStorages}, the
 * database or S3; each attachment names its storage. Photos get a small preview
 * ({@link de.codeflowwizardry.carledger.attachment.PreviewImage}).
 * <p>
 * {@link de.codeflowwizardry.carledger.attachment.AttachmentCleanup} deletes
 * the
 * content outside the database of deleted attachments (queued by a trigger),
 * {@link de.codeflowwizardry.carledger.attachment.AttachmentMover} moves files
 * into
 * the configured storage.
 */
package de.codeflowwizardry.carledger.attachment;
