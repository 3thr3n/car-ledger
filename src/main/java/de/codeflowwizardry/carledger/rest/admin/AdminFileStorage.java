package de.codeflowwizardry.carledger.rest.admin;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * The files of a storage of the attachments
 *
 * @param storage
 *            DB or S3
 * @param bytes
 *            their size together
 */
public record AdminFileStorage(@Schema(required = true) String storage, @Schema(required = true) long files,
		@Schema(required = true) long bytes)
{
}
