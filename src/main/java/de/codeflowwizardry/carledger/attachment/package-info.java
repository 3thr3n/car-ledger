/**
 * The files of the entries: receipts of the bills, documents of the
 * modifications, reports of the HUs, files of the recurring costs and the
 * pictures of the cars.
 * <p>
 * {@link de.codeflowwizardry.carledger.attachment.AttachmentService} checks the
 * access and the limits, recognizes the type by the content
 * ({@link de.codeflowwizardry.carledger.attachment.FileType}) and keeps the
 * metadata in PostgreSQL. The content goes to an
 * {@link de.codeflowwizardry.carledger.attachment.AttachmentStorage}: the
 * database or an S3 bucket, chosen by
 * {@link de.codeflowwizardry.carledger.attachment.AttachmentStorages}; each
 * attachment names its storage, so the files stay readable after a switch.
 * Photos get a small JPEG for the lists
 * ({@link de.codeflowwizardry.carledger.attachment.PreviewImage}), stored next
 * to them.
 * <p>
 * Two jobs: {@link de.codeflowwizardry.carledger.attachment.AttachmentCleanup}
 * deletes the content outside the database of every deleted attachment, which a
 * trigger queues whatever deleted it (the attachment, its entry, the car or the
 * account); {@link de.codeflowwizardry.carledger.attachment.AttachmentMover}
 * moves the existing files into the configured storage when asked to.
 */
package de.codeflowwizardry.carledger.attachment;
