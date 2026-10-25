package de.codeflowwizardry.carledger.rest.records;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * The limits of the files, as set up by the operator
 *
 * @param maxSize
 *            bytes of the largest file
 * @param maxFiles
 *            files of an entry, a recurring cost, a modification or an HU
 */
public record AttachmentLimits(@Schema(required = true) long maxSize, @Schema(required = true) int maxFiles)
{
}
