/**
 * The backup of the own data as ZIP and its restore into an account without
 * cars, all at once or nothing:
 * {@link de.codeflowwizardry.carledger.backup.BackupService} writes and reads
 * backup.json ({@link de.codeflowwizardry.carledger.backup.BackupFile}) with
 * the files of the attachments and the track points,
 * {@link de.codeflowwizardry.carledger.backup.BackupArchive} unpacks a ZIP to
 * restore entry by entry and guards against ZIP bombs.
 * <p>
 * Only the cars the user owns are backed up, with the entries of all who
 * entered them; the accesses of others come back as invitations.
 */
package de.codeflowwizardry.carledger.backup;
